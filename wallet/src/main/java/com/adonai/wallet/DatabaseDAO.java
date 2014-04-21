package com.adonai.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Entity;
import com.adonai.wallet.entities.Operation;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Database always contains current working copy
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
        GUID,
        DATA_ID,
        DATA_TYPE,
        ACTION_TYPE,
        ACTION_TIMESTAMP,
        ORIGINAL_DATA,
    }

    public static enum EntityType {
        ACCOUNT,
        CATEGORY,
        OPERATION
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

        sqLiteDatabase.execSQL("CREATE TABLE " + Account.TABLE_NAME + " (" +
                AccountFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                AccountFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                AccountFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                AccountFields.CURRENCY + " TEXT DEFAULT 'RUB' NOT NULL, " +
                AccountFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                AccountFields.COLOR + " TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "ACCOUNT_NAME_IDX ON " + Account.TABLE_NAME + " (" + AccountFields.NAME + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + Operation.TABLE_NAME + " (" +
                OperationsFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                OperationsFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                OperationsFields.CATEGORY + " INTEGER NOT NULL, " +
                OperationsFields.TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                OperationsFields.CHARGER + " INTEGER DEFAULT NULL, " +
                OperationsFields.RECEIVER + " INTEGER DEFAULT NULL, " +
                OperationsFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                OperationsFields.CONVERT_RATE + " REAL DEFAULT NULL, " +
                " FOREIGN KEY (" + OperationsFields.CATEGORY + ") REFERENCES " + Category.TABLE_NAME + " (" + CategoriesFields._id + ") ON DELETE SET NULL," +
                " FOREIGN KEY (" + OperationsFields.CHARGER + ") REFERENCES " + Account.TABLE_NAME + " (" + AccountFields._id + ") ON DELETE CASCADE," + // delete associated transactions
                " FOREIGN KEY (" + OperationsFields.RECEIVER + ") REFERENCES " + Account.TABLE_NAME + " (" + AccountFields._id + ") ON DELETE CASCADE" + // delete associated transactions
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + Currency.TABLE_NAME + " (" +
                CurrenciesFields.CODE + " TEXT PRIMARY KEY NOT NULL, " +
                CurrenciesFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                CurrenciesFields.USED_IN + " TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CURRENCY_IDX ON " + Currency.TABLE_NAME + " (" +  CurrenciesFields.CODE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + Category.TABLE_NAME + " (" +
                CategoriesFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CategoriesFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                CategoriesFields.TYPE + " INTEGER DEFAULT 0 NOT NULL, " +
                CategoriesFields.PREFERRED_ACCOUNT + " INTEGER DEFAULT NULL, " +
                " FOREIGN KEY (" + CategoriesFields.PREFERRED_ACCOUNT + ") REFERENCES " + Account.TABLE_NAME + " (" + AccountFields._id + ") ON DELETE SET NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CATEGORY_UNIQUE_NAME_IDX ON " + Category.TABLE_NAME + " (" +  CategoriesFields.NAME + "," + CategoriesFields.TYPE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + ACTIONS_TABLE_NAME + " (" +
                ActionsFields.GUID + " INTEGER DEFAULT NULL, " + // action ID is null till not synced
                ActionsFields.DATA_ID + " INTEGER NOT NULL, " +
                ActionsFields.DATA_TYPE + " INTEGER NOT NULL, " +
                ActionsFields.ACTION_TYPE + " INTEGER NOT NULL, " +
                ActionsFields.ACTION_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP PRIMARY KEY, " +
                ActionsFields.ORIGINAL_DATA + " TEXT" +
                ")");

        sqLiteDatabase.beginTransaction(); // initial fill
        // fill Categories
        final String[] defaultOutcomeCategories = mContext.getResources().getStringArray(R.array.out_categories);
        final String[] defaultIncomeCategories = mContext.getResources().getStringArray(R.array.inc_categories);
        for(final String outCategory : defaultOutcomeCategories) {
            final ContentValues values = new ContentValues(2);
            values.put(CategoriesFields.NAME.toString(), outCategory);
            values.put(CategoriesFields.TYPE.toString(), Category.EXPENSE);
            sqLiteDatabase.insert(Category.TABLE_NAME, null, values);
        }
        for(final String inCategory : defaultIncomeCategories) {
            final ContentValues values = new ContentValues(2);
            values.put(CategoriesFields.NAME.toString(), inCategory);
            values.put(CategoriesFields.TYPE.toString(), Category.INCOME);
            sqLiteDatabase.insert(Category.TABLE_NAME, null, values);
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
        final Random rand = new Random();
        for(int i = 0; i < 100; ++i) {
            final ContentValues values = new ContentValues(5);
            values.put(AccountFields.NAME.toString(), "Account" + i);
            values.put(AccountFields.DESCRIPTION.toString(), "");
            values.put(AccountFields.CURRENCY.toString(), "RUB");
            values.put(AccountFields.AMOUNT.toString(), String.valueOf(rand.nextInt(1000)));
            values.put(AccountFields.COLOR.toString(), Color.rgb(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));

            sqLiteDatabase.insert(Account.TABLE_NAME, null, values);
        }

        for(int i = 0; i < 1000; ++i) {
            final ContentValues values = new ContentValues(7);
            values.put(OperationsFields.DESCRIPTION.toString(), ""); // mandatory
            values.put(OperationsFields.CATEGORY.toString(), 3); // mandatory
            values.put(OperationsFields.AMOUNT.toString(), String.valueOf(rand.nextInt(500))); // mandatory
            values.put(OperationsFields.CHARGER.toString(), 1+rand.nextInt(90));

            sqLiteDatabase.insert(Operation.TABLE_NAME, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public long insert(ContentValues cv, String tableName) {
        long result = mDatabase.insert(tableName, null, cv);
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

    public int delete(Long id, String tableName) {
        int count = mDatabase.delete(tableName, "_id = ?", new String[]{String.valueOf(id)});
        if(count > 0)
            notifyListeners(tableName);
        return count;
    }

    public Cursor select(String query, String[] args) {
        return mDatabase.rawQuery(query, args);
    }

    /**
     * Adds and applies action
     * @param type  type of action to add
     * @param entity optional entity (for deletion ID and entityType are needed)
     * @return
     */
    public boolean makeAction(ActionType type, Entity entity) {
        Log.d("makeAction", String.format("Entity type %s, action type %s", entity.getEntityType().toString(), type.toString()));
        boolean status = false;
        mDatabase.beginTransaction();
        transactionFlow:
        {
            final ContentValues values = new ContentValues(5);
            values.put(ActionsFields.DATA_TYPE.toString(), entity.getEntityType().ordinal());
            values.put(ActionsFields.ACTION_TYPE.toString(), type.ordinal());
            switch (type) {
                case ADD: { // we're adding, only store ID to keep track of it
                    final long persistedId = entity.persist(this); // result holds ID now
                    if (persistedId == -1) // error
                        break transactionFlow;
                    entity.setId(persistedId); // we should track new ID of entity in action fields

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
                        case ACCOUNT:
                            oldEntity = getAccount(entity.getId());
                            break;
                        case CATEGORY:
                            oldEntity = getCategory(entity.getId());
                            break;
                        case OPERATION:
                            oldEntity = getOperation(entity.getId());
                            break;
                        default:
                            throw new IllegalArgumentException("No such entity type!" + entity.getEntityType());
                    }

                    // second, update old row with new data
                    final long rowsUpdated = entity.update(this); // result holds updated entities count now
                    if (rowsUpdated != 1) // error
                        break transactionFlow;

                    // third, do we have original already?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entity.getEntityType().ordinal())}, null, null, null, null);
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
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entity.getEntityType().ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) { // we already have this entity stored, added or modified, need to update previous action
                        final int previousActionType = cursor.getInt(ActionsFields.ACTION_TYPE.ordinal());
                        if(previousActionType == ActionType.ADD.ordinal()) { // if entity was added, we should just delete action, so all will look like nothing happened
                            final long actionsDeleted = mDatabase.delete(ACTIONS_TABLE_NAME, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entity.getEntityType().ordinal())});
                            if(actionsDeleted != 1)
                                break transactionFlow;
                        } else if(previousActionType == ActionType.MODIFY.ordinal()) { // change action type to deleted
                            values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                            final long actionsUpdated = mDatabase.update(ACTIONS_TABLE_NAME, values, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entity.getEntityType().ordinal())});
                            if(actionsUpdated != 1)
                                break transactionFlow;
                        } else
                            throw new IllegalArgumentException("Can't delete already deleted item!");
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

    public Account getAccount(long id) {
        final Cursor cursor = mDatabase.query(Account.TABLE_NAME, null, " _id = ?", new String[]{String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit
        if (cursor.moveToFirst()) {
            final Account acc = new Account();
            acc.setId(cursor.getLong(AccountFields._id.ordinal()));
            acc.setName(cursor.getString(AccountFields.NAME.ordinal()));
            acc.setDescription(cursor.getString(AccountFields.DESCRIPTION.ordinal()));
            acc.setCurrency(getCurrency(cursor.getString(AccountFields.CURRENCY.ordinal())));
            acc.setAmount(new BigDecimal(cursor.getString(AccountFields.AMOUNT.ordinal())));
            acc.setColor(cursor.getInt(AccountFields.COLOR.ordinal()));

            Log.d("getAccount(" + id + ")", acc.getName());
            cursor.close();
            return acc;
        }

        cursor.close();
        return null;
    }

    public Cursor getAccountCursor() {
        Log.d("Query", "getAccountCursor");
        return mDatabase.query(Account.TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getCurrencyCursor() {
        Log.d("Query", "getCurrencyCursor");
        return mDatabase.query(Currency.TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getOperationsCursor() {
        Log.d("Query", "getOperationsCursor");
        return mDatabase.query(Operation.TABLE_NAME, null, null, null, null, null, null, null);
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
        filterBuilder.setTables(Operation.TABLE_NAME + " AS op" +
                " LEFT JOIN " + Account.TABLE_NAME + " AS charger " + "ON op." + OperationsFields.CHARGER + " = " + "charger." + AccountFields._id +
                " LEFT JOIN " + Account.TABLE_NAME + " AS benefic " + "ON op." + OperationsFields.CHARGER + " = " + "benefic." + AccountFields._id);
        return filterBuilder.query(mDatabase, new String[]{"op.*"}, null, new String[] {"%" + filter + "%"}, null, null, null);
        //mDatabase.query(TABLE_NAME, null, sb.toString(), new String[]{"%" + filter + "%"}, null, null, null, null);
    }

    public Cursor getCategoryCursor() {
        Log.d("Query", "getCategoryCursor");
        return mDatabase.query(Category.TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getCategoryCursor(int type) {
        Log.d("Query", "getCategoryCursorWithType");
        return mDatabase.query(Category.TABLE_NAME, null, CategoriesFields.TYPE + " = ?", new String[]{String.valueOf(type)}, null, null, null, null);
    }

    public Operation getOperation(long id) {
        final Long preciseTime = System.currentTimeMillis();
        final Cursor cursor = mDatabase.query(Operation.TABLE_NAME, null, OperationsFields._id + " = ?", new String[] {String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit
        if (cursor.moveToFirst()) try {
            final Operation op = new Operation();
            op.setId(cursor.getLong(OperationsFields._id.ordinal()));
            op.setDescription(cursor.getString(OperationsFields.DESCRIPTION.ordinal()));
            op.setCategory(getCategory(cursor.getLong(OperationsFields.CATEGORY.ordinal())));
            op.setTime(Utils.SQLITE_DATE_FORMAT.parse(cursor.getString(OperationsFields.TIME.ordinal())));
            op.setCharger(getAccount(cursor.getInt(OperationsFields.CHARGER.ordinal())));
            if(!cursor.isNull(OperationsFields.RECEIVER.ordinal()))
                op.setBeneficiar(getAccount(cursor.getInt(OperationsFields.RECEIVER.ordinal())));
            op.setAmount(new BigDecimal(cursor.getString(OperationsFields.AMOUNT.ordinal())));
            if(!cursor.isNull(OperationsFields.CONVERT_RATE.ordinal()))
                op.setConvertingRate(cursor.getDouble(OperationsFields.CONVERT_RATE.ordinal()));
            cursor.close();

            Log.d(String.format("getOperation(%d), took %d ms", id, System.currentTimeMillis() - preciseTime), op.getAmount().toPlainString());
            return op;
        } catch (ParseException ex) {
            throw new RuntimeException(ex); // should never happen
        }

        cursor.close();
        return null;
    }

    public Category getCategory(long id) {
        final Cursor cursor = mDatabase.query(Category.TABLE_NAME, null, CategoriesFields._id + " = ?", new String[] {String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit
        if (cursor.moveToFirst()) {
            // 4. build operation object
            final Category cat = new Category();
            cat.setId(cursor.getLong(CategoriesFields._id.ordinal()));
            cat.setName(cursor.getString(CategoriesFields.NAME.ordinal()));
            cat.setType(cursor.getInt(CategoriesFields.TYPE.ordinal()));
            if(!cursor.isNull(CategoriesFields.PREFERRED_ACCOUNT.ordinal()))
                cat.setPreferredAccount(getAccount(cursor.getLong(CategoriesFields.PREFERRED_ACCOUNT.ordinal())));
            cursor.close();

            Log.d(String.format("getCategory(%d)", id), cat.getName());
            return cat;
        }

        cursor.close();
        return null;
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
    public void getAsyncOperation(final long id, AsyncDbQuery.Listener<Operation> lst) {
        new AsyncDbQuery<>(lst).execute(new Callable<Operation>() {
            @Override
            public Operation call() throws Exception {
                return getOperation(id);
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

            notifyListeners(Operation.TABLE_NAME);
            notifyListeners(Account.TABLE_NAME);
        }
        mDatabase.endTransaction();
        return allSucceeded;
    }

    public boolean revertOperation(Long id) {
        return revertOperation(getOperation(id));
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

    @SuppressWarnings("unchecked")
    public <T extends Entity> T getLocallyModified(T entity) {
        final Cursor actionCursor = mDatabase.query(ACTIONS_TABLE_NAME,
                new String[]{ActionsFields.ORIGINAL_DATA.toString()},
                ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ? AND " + ActionsFields.ACTION_TYPE + " = " + ActionType.MODIFY.ordinal(),
                new String[]{String.valueOf(entity.getId()), String.valueOf(entity.getEntityType().ordinal())}, null, null, null);
        if(actionCursor.moveToNext()) {
            final String json = actionCursor.getString(0);
            final Entity modifiedEntity = new Gson().fromJson(json, entity.getClass());
            return (T) modifiedEntity;
        }

        return null;
    }
}

