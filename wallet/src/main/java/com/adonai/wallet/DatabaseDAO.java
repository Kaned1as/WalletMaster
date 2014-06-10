package com.adonai.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Entity;
import com.adonai.wallet.entities.EntityDescriptor;
import com.adonai.wallet.entities.Operation;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Database helper instance
 *
 * Database entity tables always contains current working copy
 * Actions contain original data
 */
public class DatabaseDAO extends SQLiteOpenHelper
{
    private final Context mContext;

    public interface DatabaseListener {
        void handleUpdate();
    }

    public void registerDatabaseListener(final String table, final DatabaseListener listener) {
        if(listenerMap.containsKey(table))
            listenerMap.get(table).add(listener);
        else
            listenerMap.put(table, new ArrayList<DatabaseListener>() {{ add(listener); }});
    }

    public void unregisterDatabaseListener(final String table, final DatabaseListener listener) {
        if(listenerMap.containsKey(table))
            listenerMap.get(table).remove(listener);
    }

    public static final String dbName = "moneyDB";
    public static final int dbVersion = 1;

    public static enum AccountFields {
        _id,
        NAME,
        DESCRIPTION,
        CURRENCY,
        AMOUNT,
        COLOR,
    }

    public static enum OperationsFields {
        _id,
        DESCRIPTION,
        CATEGORY,
        TIME,
        CHARGER,
        RECEIVER,
        AMOUNT,
        CONVERT_RATE,
    }

    public static enum CurrenciesFields {
        CODE,
        DESCRIPTION,
        USED_IN,
    }


    public static enum CategoriesFields {
        _id,
        NAME,
        TYPE,
        PREFERRED_ACCOUNT,
    }

    public static final String ACTIONS_TABLE_NAME = "actions";
    public static enum ActionsFields {
        DATA_ID,
        DATA_TYPE,
        ORIGINAL_DATA,
    }

    public static enum EntityType {
        ACCOUNTS,
        CATEGORIES,
        OPERATIONS
    }

    public static enum ActionType {
        ADD,
        DELETE,
        MODIFY
    }

    private final Map<String, List<DatabaseListener>> listenerMap = new HashMap<>();
    private SQLiteDatabase mDatabase;

    public DatabaseDAO(Context context) {
        super(context, dbName, null, dbVersion);
        mContext = context;
        mDatabase = getWritableDatabase();
        assert mDatabase != null;
    }

    @Override
    public void onOpen(SQLiteDatabase db) { // called AFTER upgrade!
        super.onOpen(db);
        if (!db.isReadOnly())
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        mDatabase = sqLiteDatabase;

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.ACCOUNTS + " (" +
                AccountFields._id + " TEXT PRIMARY KEY, " +
                AccountFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                AccountFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                AccountFields.CURRENCY + " TEXT DEFAULT 'RUB' NOT NULL, " +
                AccountFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                AccountFields.COLOR + " TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "ACCOUNT_NAME_IDX ON " + EntityType.ACCOUNTS + " (" + AccountFields.NAME + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.OPERATIONS + " (" +
                OperationsFields._id + " TEXT PRIMARY KEY, " +
                OperationsFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                OperationsFields.CATEGORY + " TEXT NOT NULL, " +
                OperationsFields.TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                OperationsFields.CHARGER + " TEXT DEFAULT NULL, " +
                OperationsFields.RECEIVER + " TEXT DEFAULT NULL, " +
                OperationsFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                OperationsFields.CONVERT_RATE + " REAL DEFAULT NULL, " +
                " FOREIGN KEY (" + OperationsFields.CATEGORY + ") REFERENCES " + EntityType.CATEGORIES + " (" + CategoriesFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE," +
                " FOREIGN KEY (" + OperationsFields.CHARGER + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE," + // delete associated transactions
                " FOREIGN KEY (" + OperationsFields.RECEIVER + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" + // delete associated transactions
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + Currency.TABLE_NAME + " (" +
                CurrenciesFields.CODE + " TEXT PRIMARY KEY, " +
                CurrenciesFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                CurrenciesFields.USED_IN + " TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CURRENCY_IDX ON " + Currency.TABLE_NAME + " (" +  CurrenciesFields.CODE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.CATEGORIES + " (" +
                CategoriesFields._id + " TEXT PRIMARY KEY, " +
                CategoriesFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                CategoriesFields.TYPE + " INTEGER DEFAULT 0 NOT NULL, " +
                CategoriesFields.PREFERRED_ACCOUNT + " TEXT DEFAULT NULL, " +
                " FOREIGN KEY (" + CategoriesFields.PREFERRED_ACCOUNT + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE SET NULL ON UPDATE CASCADE" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CATEGORY_UNIQUE_NAME_IDX ON " + EntityType.CATEGORIES + " (" +  CategoriesFields.NAME + "," + CategoriesFields.TYPE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + ACTIONS_TABLE_NAME + " (" +
                ActionsFields.DATA_ID + " TEXT NOT NULL, " +
                ActionsFields.DATA_TYPE + " INTEGER NOT NULL, " +
                ActionsFields.ORIGINAL_DATA + " TEXT, " +
                " PRIMARY KEY(" + ActionsFields.DATA_ID + ", " + ActionsFields.DATA_TYPE + ")" +
                ")");

        sqLiteDatabase.beginTransaction(); // initial fill
        // fill Categories
        final String[] defaultOutcomeCategories = mContext.getResources().getStringArray(R.array.out_categories);
        final String[] defaultIncomeCategories = mContext.getResources().getStringArray(R.array.inc_categories);
        final String[] defaultTransCategories = mContext.getResources().getStringArray(R.array.transfer_categories);
        for(final String outCategory : defaultOutcomeCategories) {
            final ContentValues values = new ContentValues(3);
            final Category toAdd = new Category();
            toAdd.setName(outCategory);
            toAdd.setType(Category.EXPENSE);
            makeAction(ActionType.ADD, toAdd);
        }
        for(final String inCategory : defaultIncomeCategories) {
            final Category toAdd = new Category();
            toAdd.setName(inCategory);
            toAdd.setType(Category.INCOME);
            makeAction(ActionType.ADD, toAdd);
        }
        for(final String transCategory : defaultTransCategories) {
            final Category toAdd = new Category();
            toAdd.setName(transCategory);
            toAdd.setType(Category.TRANSFER);
            makeAction(ActionType.ADD, toAdd);
        }

        //fill Currencies
        final InputStream allCurrencies = getClass().getResourceAsStream("/assets/currencies.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(allCurrencies));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                final String[] tokens = line.split(":");
                final ContentValues values = new ContentValues(3);
                switch (tokens.length) { // switch-case-no-break magic!
                    case 3:
                        values.put(CurrenciesFields.USED_IN.toString(), tokens[2]);
                    case 2:
                        values.put(CurrenciesFields.DESCRIPTION.toString(), tokens[1]);
                    case 1:
                        values.put(CurrenciesFields.CODE.toString(), tokens[0]);
                        break;
                }
                sqLiteDatabase.insert(Currency.TABLE_NAME, null, values);
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // should not happen!
        }

        sqLiteDatabase.setTransactionSuccessful(); // batch insert
        sqLiteDatabase.endTransaction();
        // fill

        // test accounts
        /*
        final Random rand = new Random();
        for(int i = 0; i < 100; ++i) {
            final ContentValues values = new ContentValues(5);
            values.put(AccountFields._id.toString(), UUID.randomUUID().toString());
            values.put(AccountFields.NAME.toString(), "Account" + i);
            values.put(AccountFields.DESCRIPTION.toString(), "");
            values.put(AccountFields.CURRENCY.toString(), "RUB");
            values.put(AccountFields.AMOUNT.toString(), String.valueOf(rand.nextInt(1000)));
            values.put(AccountFields.COLOR.toString(), Color.rgb(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));

            sqLiteDatabase.insert(EntityType.ACCOUNTS.toString(), null, values);
        }

        for(int i = 0; i < 1000; ++i) {
            final ContentValues values = new ContentValues(7);
            values.put(OperationsFields._id.toString(), UUID.randomUUID().toString());
            values.put(OperationsFields.DESCRIPTION.toString(), ""); // mandatory
            values.put(OperationsFields.CATEGORY.toString(), 3); // mandatory
            values.put(OperationsFields.AMOUNT.toString(), String.valueOf(rand.nextInt(500))); // mandatory
            values.put(OperationsFields.CHARGER.toString(), 1+rand.nextInt(90));

            sqLiteDatabase.insert(EntityType.OPERATIONS.toString(), null, values);
        }
        */
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public long insert(ContentValues cv, String tableName) {
        long result = mDatabase.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if(result != -1)
            notifyListeners(tableName);
        return result;
    }

    public int update(ContentValues cv, String tableName) {
        final int result = mDatabase.update(tableName,  cv,  "_id = ?",  new String[] { cv.getAsString("_id") });
        if(result > 0)
            notifyListeners(tableName);
        return result;
    }

    public int delete(String id, String tableName) {
        int count = mDatabase.delete(tableName, "_id = ?", new String[]{String.valueOf(id)});
        if(count > 0)
            notifyListeners(tableName);
        return count;
    }

    /**
     * Retrieves cursor of raw query request to underlying database
     * Don't forget to close the cursor!
     * @param query raw query to database
     * @param args query arguments
     * @return cursor of resulting query
     */
    public Cursor select(String query, String[] args) {
        return mDatabase.rawQuery(query, args);
    }

    /**
     * Retrieves cursor of direct query by id
     * Don't forget to close the cursor!
     * @param entityType type of entity to retrieve
     * @param id uuid of entity to retrieve
     * @return cursor of resulting query
     */
    public Cursor get(EntityType entityType, String id) {
        return mDatabase.query(entityType.toString(), null, " _id = ?", new String[]{String.valueOf(id)}, null, null, null, null);
    }

    /**
     * Adds and applies action
     * @param type  type of action to add
     * @param entity optional entity (for deletion ID and entityType are needed)
     * @return result of inserting new operation
     */
    public boolean makeAction(ActionType type, Entity entity) {
        final EntityType entityType = entity.getClass().getAnnotation(EntityDescriptor.class).type();
        Log.d("makeAction", String.format("Entity type %s, action type %s", entityType.toString(), type.toString()));
        boolean status = false;
        mDatabase.beginTransaction();
        transactionFlow: {
            final ContentValues values = new ContentValues(3);
            values.put(ActionsFields.DATA_TYPE.toString(), entityType.ordinal());
            switch (type) {
                case ADD: { // we're adding, only store ID to keep track of it
                    // first, persist entity
                    final String persistedId = entity.persist(this); // result holds ID now
                    if (persistedId == null) // error
                        break transactionFlow;
                    entity.setId(persistedId); // we should track new ID of entity in action fields

                    // second, do we have this data in any actions?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {entity.getId(), String.valueOf(entityType.ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) // we already have this entity deleted (reapplying operation), nothing to do
                        break;

                    values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                    values.put(ActionsFields.ORIGINAL_DATA.toString(), (byte[]) null);
                    final long actionRow = mDatabase.insert(ACTIONS_TABLE_NAME, null, values);
                    if(actionRow == -1)
                        break transactionFlow;
                    break;
                }
                case MODIFY: { // we are modifying, need to backup original!
                    // first, retrieve old entity
                    final Entity oldEntity;
                    switch (entity.getEntityType()) {
                        case ACCOUNTS:
                            oldEntity = Account.getFromDB(this, entity.getId());
                            break;
                        case CATEGORIES:
                            oldEntity = Category.getFromDB(this, entity.getId());
                            break;
                        case OPERATIONS:
                            oldEntity = Operation.getFromDB(this, entity.getId());
                            break;
                        default:
                            throw new IllegalArgumentException("No such entity type!" + entity.getEntityType());
                    }

                    // second, update old row with new data
                    final long rowsUpdated = entity.update(this); // result holds updated entities count now
                    if (rowsUpdated != 1) // error
                        break transactionFlow;

                    // third, do we have original already?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {entity.getId(), String.valueOf(entityType.ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) // we already have this entity stored (added or modified), nothing to do
                        break;

                    values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                    values.put(ActionsFields.ORIGINAL_DATA.toString(), new Gson().toJson(oldEntity));
                    final long insertedActionId = mDatabase.insert(ACTIONS_TABLE_NAME, null, values);
                    if(insertedActionId == -1)
                        break transactionFlow;
                    break;
                }
                case DELETE: { // we are deleting need to store all original data
                    // first delete entity from DB
                    final long entitiesDeleted = entity.delete(this); // result holds updated entities count now
                    if (entitiesDeleted != 1) // error
                        break transactionFlow;

                    // second, do we have this data in any actions?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entityType.ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) { // we already have this entity stored, added or modified, need to update previous action
                        final String backedEntity = cursor.getString(ActionsFields.ORIGINAL_DATA.ordinal());
                        if(backedEntity == null) { // if entity was added, we should just delete action, so all will look like nothing happened
                            final long actionsDeleted = mDatabase.delete(ACTIONS_TABLE_NAME, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entityType.ordinal())});
                            if(actionsDeleted != 1)
                                break transactionFlow;
                        } else { // was modified, change action type to deleted
                            values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                            final long actionsUpdated = mDatabase.update(ACTIONS_TABLE_NAME, values, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entityType.ordinal())});
                            if(actionsUpdated != 1)
                                break transactionFlow;
                        }
                    } else { // we don't have any actions, should create and store original
                        values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                        values.put(ActionsFields.ORIGINAL_DATA.toString(), new Gson().toJson(entity));
                        final long actionRow = mDatabase.insert(ACTIONS_TABLE_NAME, null, values);
                        if(actionRow == -1)
                            break transactionFlow;
                    }
                    break;
                }
            }

            // all succeeded
            mDatabase.setTransactionSuccessful();
            status = true;
        }
        mDatabase.endTransaction();

        return status;
    }

    public Cursor getAccountCursor() {
        Log.d("Query", "getAccountCursor");
        return mDatabase.query(EntityType.ACCOUNTS.toString(), null, null, null, null, null, null, null);
    }

    public Cursor getCurrencyCursor() {
        Log.d("Query", "getCurrencyCursor");
        return mDatabase.query(Currency.TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getOperationsCursor() {
        Log.d("Query", "getOperationsCursor");
        return mDatabase.query(EntityType.OPERATIONS.toString(), null, null, null, null, null, OperationsFields.TIME + " ASC", null);
    }

    public Cursor getEntityCursor(String tableName, long id) {
        Log.d("Query", "getOperationsCursor");
        return mDatabase.query(tableName, null, " _id = ?", new String[]{String.valueOf(id)}, null, null, null, null);
    }

    public Cursor getOperationsCursor(String filter) {
        Log.d("Query", "getOperationsCursor with filter");
        final SQLiteQueryBuilder filterBuilder = new SQLiteQueryBuilder();

        final StringBuilder sb = new StringBuilder(20);
        sb.append("LOWER(");
        sb.append("COALESCE(op.").append(OperationsFields.DESCRIPTION).append(", '')");
        sb.append(" || COALESCE(op.").append(OperationsFields.AMOUNT).append(", '')");
        sb.append(" || COALESCE(op.").append(OperationsFields.TIME).append(", '')");
        sb.append(" || COALESCE(charger.").append(AccountFields.NAME).append(", '')");
        sb.append(" || COALESCE(benefic.").append(AccountFields.NAME).append(", '')");

        sb.append(") LIKE LOWER(?)");
        filterBuilder.appendWhere(sb.toString());
        filterBuilder.setTables(EntityType.OPERATIONS + " AS op" +
                " LEFT JOIN " + EntityType.ACCOUNTS + " AS charger " + "ON op." + OperationsFields.CHARGER + " = " + "charger." + AccountFields._id +
                " LEFT JOIN " + EntityType.ACCOUNTS + " AS benefic " + "ON op." + OperationsFields.CHARGER + " = " + "benefic." + AccountFields._id);
        return filterBuilder.query(mDatabase, new String[]{"op.*"}, null, new String[] {"%" + filter + "%"}, null, null, null);
        //mDatabase.query(tableName, null, sb.toString(), new String[]{"%" + filter + "%"}, null, null, null, null);
    }

    public Cursor getCategoryCursor() {
        Log.d("Query", "getCategoryCursor");
        return mDatabase.query(EntityType.CATEGORIES.toString(), null, null, null, null, null, CategoriesFields.NAME + " ASC", null);
    }

    public Cursor getCategoryCursor(int type) {
        Log.d("Query", "getCategoryCursorWithType");
        return mDatabase.query(EntityType.CATEGORIES.toString(), null, CategoriesFields.TYPE + " = ?", new String[]{String.valueOf(type)}, null, null, CategoriesFields.NAME + " ASC", null);
    }

    public void notifyListeners(String table) {
        if(listenerMap.containsKey(table))
            for(final DatabaseListener listener : listenerMap.get(table))
                listener.handleUpdate();
    }

    public long addCurrency(Currency curr) {
        Log.d("addCurrency", curr.toString());
        final ContentValues values = new ContentValues(3);
        values.put(CurrenciesFields.CODE.toString(), curr.getDescription());
        if(curr.getDescription() != null)
            values.put(CurrenciesFields.DESCRIPTION.toString(), curr.getDescription());
        if(curr.getUsedIn() != null)
            values.put(CurrenciesFields.USED_IN.toString(), curr.getUsedIn());

        return mDatabase.insert(Currency.TABLE_NAME, null, values);
    }

    public Currency getCurrency(String code) {
        Log.d("getCurrency", code);
        final Cursor cursor = mDatabase.query(Currency.TABLE_NAME, null, CurrenciesFields.CODE + " = ?", new String[]{code}, null, null, null, null);

        if (cursor.moveToNext()) {
            final Currency result = new Currency(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            cursor.close();
            return result;
        }

        cursor.close();
        return null;
    }

    public static class AsyncDbQuery<T> extends AsyncTask<Callable<T>, Void, T> {
        private final Listener<T> listener;

        protected AsyncDbQuery(Listener<T> listener) {
            this.listener = listener;
        }

        public interface Listener<T> {
            void onFinishLoad(T what);
        }

        @Override
        @SafeVarargs
        final protected T doInBackground(Callable<T>... params) {
            try {
                return params[0].call();
            } catch (Exception e) {
                // should not happen!
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(T t) {
            super.onPostExecute(t);
            listener.onFinishLoad(t);
        }
    }

    @SuppressWarnings("unchecked")
    public void getAsyncOperation(final String id, AsyncDbQuery.Listener<Operation> lst) {
        new AsyncDbQuery<>(lst).execute(new Callable<Operation>() {
            @Override
            public Operation call() throws Exception {
                return Operation.getFromDB(DatabaseDAO.this, id);
            }
        });
    }

    /**
     * Calling this method means we have full operation object with all data built and ready for applying
     * @param operation operation to be applied
     */
    public boolean applyOperation(Operation operation) {
        final Account chargeAcc = operation.getCharger();
        final Account benefAcc = operation.getBeneficiar();
        final BigDecimal amount = operation.getAmount();

        boolean allSucceeded = false;
        mDatabase.beginTransaction();
        transactionFlow:
        {
            if(!makeAction(ActionType.ADD, operation))
                break transactionFlow;

            switch (operation.getOperationType()) {
                case TRANSFER:
                    benefAcc.setAmount(benefAcc.getAmount().add(operation.getAmountDelivered()));
                    chargeAcc.setAmount(chargeAcc.getAmount().subtract(amount));
                    if(!makeAction(ActionType.MODIFY, benefAcc) || !makeAction(ActionType.MODIFY, chargeAcc)) // apply to db
                        break transactionFlow;
                    break;
                case EXPENSE: // subtract value
                    chargeAcc.setAmount(chargeAcc.getAmount().subtract(amount));
                    if(!makeAction(ActionType.MODIFY, chargeAcc))
                        break transactionFlow;
                    break;
                case INCOME: // add value
                    benefAcc.setAmount(benefAcc.getAmount().add(amount));
                    if(!makeAction(ActionType.MODIFY, benefAcc))
                        break transactionFlow;
                    break;
            }
            mDatabase.setTransactionSuccessful();
            allSucceeded = true;
        }
        mDatabase.endTransaction();
        return allSucceeded;
    }

    /**
     * Call this in need of explicitly selecting operation from DB to have up-to-date values such when
     * reapplying operation
     * @param id id of operation to revert
     * @return success or failure
     */
    public boolean revertOperation(String id) {
        return revertOperation(Operation.getFromDB(this, id));
    }

    /**
     * Calling this method means we have full operation object with all data built and ready for reverting
     * @param operation operation to be reverted
     */
    public boolean revertOperation(Operation operation) {
        final Account chargeAcc = operation.getCharger();
        final Account benefAcc = operation.getBeneficiar();
        final BigDecimal amount = operation.getAmount();

        boolean allSucceeded = false;
        mDatabase.beginTransaction();
        transactionFlow:
        {
            if(!makeAction(ActionType.DELETE, operation))
                break transactionFlow;

            switch (operation.getOperationType()) {
                case TRANSFER:
                    benefAcc.setAmount(benefAcc.getAmount().subtract(operation.getAmountDelivered()));
                    chargeAcc.setAmount(chargeAcc.getAmount().add(amount));
                    if(!makeAction(ActionType.MODIFY, benefAcc) || !makeAction(ActionType.MODIFY, chargeAcc))
                        break transactionFlow;
                    break;
                case EXPENSE: // add subtracted value
                    chargeAcc.setAmount(chargeAcc.getAmount().add(amount));
                    if(!makeAction(ActionType.MODIFY, chargeAcc))
                        break transactionFlow;
                    break;
                case INCOME: // subtract added value
                    benefAcc.setAmount(benefAcc.getAmount().subtract(amount));
                    if(!makeAction(ActionType.MODIFY, benefAcc))
                        break transactionFlow;
                    break;
            }
            mDatabase.setTransactionSuccessful();
            allSucceeded = true;
        }
        mDatabase.endTransaction();
        return allSucceeded;
    }

    /**
     * We have locally modified entity in one action only
     * @param entity entity that should be found in action list
     * @param <T> type of entity to be found
     * @return found entity with modification type or null if nothing was found
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> T getBackedVersion(T entity) {
        final Cursor actionCursor = mDatabase.query(ACTIONS_TABLE_NAME,
                new String[]{ActionsFields.ORIGINAL_DATA.toString()},
                ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?",
                new String[]{String.valueOf(entity.getId()), String.valueOf(entity.getEntityType().ordinal())}, null, null, null);
        if(actionCursor.moveToNext()) {
            final String json = actionCursor.getString(0);
            final Entity modifiedEntity = new Gson().fromJson(json, entity.getClass());
            return (T) modifiedEntity;
        }

        return null;
    }

    /**
     * Select all entities that are known to client
     * Note that entities added on device are not in this list
     * @return list of known entities IDs
     */
    public <T extends Entity> List<String> getKnownIDs(Class<T> clazz) {
        final List<String> result = new ArrayList<>();
        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
        final Cursor selections = select(
                "SELECT " + OperationsFields._id +
                " FROM " + type.toString() +
                " WHERE " + OperationsFields._id + " NOT IN (" +
                    "SELECT " + ActionsFields.DATA_ID +
                    " FROM " + ACTIONS_TABLE_NAME +
                    " WHERE " + ActionsFields.DATA_TYPE + " = " + type.ordinal() +
                    " AND " + ActionsFields.ORIGINAL_DATA + " IS NULL" + // not in added entities
                    ")" +
                " UNION ALL " + // we also know about deleted entities, so we should add them
                "SELECT " + ActionsFields.DATA_ID +
                " FROM " + ACTIONS_TABLE_NAME +
                " WHERE " + ActionsFields.DATA_TYPE + " = " + type.ordinal() +
                " AND " + ActionsFields.DATA_ID + " NOT IN (" +
                    "SELECT " + OperationsFields._id +
                    " FROM " + type.toString() + // these are deleted entities - present in backup table but not exist in real entities table
                    ")"
                , null);
        while (selections.moveToNext())
            result.add(selections.getString(0));
        selections.close();
        return result;
    }

    /**
     * Select all entities that are added locally and not yet synchronized with remote database
     * @return list of entities of specified class that were added locally
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> getAdded(Class<T> clazz) {
        final List<T> result = new ArrayList<>();
        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
        final Cursor selections = select(
                "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
                        " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
                        " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + type.ordinal() +
                        " AND " + DatabaseDAO.ActionsFields.ORIGINAL_DATA + " IS NULL" //  added entities
                , null
        );
        while (selections.moveToNext())
            try {
                final Method filler = clazz.getDeclaredMethod("getFromDB", DatabaseDAO.class, String.class);
                final T newEntity = (T) filler.invoke(null, this, selections.getString(0));
                if(newEntity != null)
                    result.add(newEntity);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
                throw new IllegalArgumentException("No public static getFromDB method in child entity class!");
            }

        selections.close();
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> getModified(Class<T> clazz) {
        final List<T> result = new ArrayList<>();
        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
        final Cursor selections = select(
                "SELECT " + DatabaseDAO.AccountFields._id +
                " FROM " + type +
                " WHERE " + DatabaseDAO.AccountFields._id + " IN (" +
                    "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
                    " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
                    " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + type.ordinal() +
                    " AND " + DatabaseDAO.ActionsFields.ORIGINAL_DATA + " IS NOT NULL" + // exists in original and modified version - it's modified!
                    ")"
                , null);
        while (selections.moveToNext())
            try {
                final Method filler = clazz.getDeclaredMethod("getFromDB", DatabaseDAO.class, String.class);
                final T newEntity = (T) filler.invoke(null, this, selections.getString(0));
                if(newEntity != null)
                    result.add(newEntity);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
                throw new IllegalArgumentException("No public static getFromDB method in child entity class!");
            }
        selections.close();
        return result;
    }

    public <T extends Entity> List<String> getDeleted(Class<T> clazz) {
        final List<String> result = new ArrayList<>();
        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
        final Cursor selections = select(
                "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
                        " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
                        " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + type.ordinal() +
                        " AND " + DatabaseDAO.ActionsFields.DATA_ID + " NOT IN (" +
                        "SELECT " + DatabaseDAO.AccountFields._id +
                        " FROM " + type + // these are deleted entities - present in backup table but not exist in real entities table
                        ")"
                , null
        );
        while (selections.moveToNext())
            result.add(selections.getString(0));
        selections.close();
        return result;
    }

    public int clearActions() {
        return mDatabase.delete(ACTIONS_TABLE_NAME, "1", null);
    }

    public void beginTransaction() {
        mDatabase.beginTransaction();
    }

    public void endTransaction() {
        mDatabase.endTransaction();
    }

    public void setTransactionSuccessful() {
        mDatabase.setTransactionSuccessful();
    }
}

