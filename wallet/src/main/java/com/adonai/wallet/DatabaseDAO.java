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
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
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
    private static DatabaseDAO mInstance;

    public static void init(Context context) {
        mInstance = new DatabaseDAO(context.getApplicationContext());
    }

    public static DatabaseDAO getInstance() {
        return mInstance;
    }

    public interface DatabaseListener {
        static final String ANY_TABLE = "*";

        void handleUpdate();
    }

    /**
     * Register specified listener as database listener for changes.
     * @param listener listener to be registered
     * @param table table name to check or null if all table changes should be tracked
     */
    public void registerDatabaseListener(final DatabaseListener listener, final String table) {
        if(table == null) { // listen on all
            registerDatabaseListener(listener, DatabaseListener.ANY_TABLE);
            return;
        }

        if(listenerMap.containsKey(table))
            listenerMap.get(table).add(listener);
        else {
            final ArrayList<DatabaseListener> newList = new ArrayList<>();
            newList.add(listener);
            listenerMap.put(table, newList);
        }
    }

    /**
     * Unregister specified listener
     * @param listener listener to be unregistered
     * @param table table name that listener checked or null if listener tracked any table
     */
    public void unregisterDatabaseListener(final DatabaseListener listener, final String table) {
        if(table == null) { // listen on all
            unregisterDatabaseListener(listener, DatabaseListener.ANY_TABLE);
            return;
        }

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

    public static enum BudgetFields {
        _id,
        NAME,
        START_TIME,
        END_TIME,
        COVERED_ACCOUNT
    }

    public static enum BudgetItemFields {
        _id,
        PARENT_BUDGET,
        CATEGORY,
        MAX_AMOUNT
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
        OPERATIONS,
        BUDGETS,
        BUDGET_ITEMS
    }

    private final Map<String, ArrayList<DatabaseListener>> listenerMap = new HashMap<>();
    private SQLiteDatabase mDatabase;

    private DatabaseDAO(Context context) {
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
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX ACCOUNT_NAME_IDX ON " + EntityType.ACCOUNTS + " (" + AccountFields.NAME + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.OPERATIONS + " (" +
                OperationsFields._id + " TEXT PRIMARY KEY, " +
                OperationsFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                OperationsFields.CATEGORY + " TEXT NOT NULL, " +
                OperationsFields.TIME + " TIMESTAMP DEFAULT (STRFTIME('%s', 'now') * 1000) NOT NULL, " +
                OperationsFields.CHARGER + " TEXT DEFAULT NULL, " +
                OperationsFields.RECEIVER + " TEXT DEFAULT NULL, " +
                OperationsFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                OperationsFields.CONVERT_RATE + " REAL DEFAULT NULL, " +
                " FOREIGN KEY (" + OperationsFields.CATEGORY + ") REFERENCES " + EntityType.CATEGORIES + " (" + CategoriesFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE," +
                " FOREIGN KEY (" + OperationsFields.CHARGER + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE," + // delete associated transactions
                " FOREIGN KEY (" + OperationsFields.RECEIVER + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" + // delete associated transactions
                ")");
        sqLiteDatabase.execSQL("CREATE INDEX OP_TIME_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.TIME + ")");
        sqLiteDatabase.execSQL("CREATE INDEX OP_CATEGORY_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.CATEGORY + ")");
        sqLiteDatabase.execSQL("CREATE INDEX OP_CHARGER_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.CHARGER + ")");
        sqLiteDatabase.execSQL("CREATE INDEX OP_RECEIVER_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.RECEIVER + ")");
        sqLiteDatabase.execSQL("CREATE INDEX OP_AMOUNT_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.AMOUNT + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + Currency.TABLE_NAME + " (" +
                CurrenciesFields.CODE + " TEXT PRIMARY KEY, " +
                CurrenciesFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                CurrenciesFields.USED_IN + " TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX CURRENCY_IDX ON " + Currency.TABLE_NAME + " (" +  CurrenciesFields.CODE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.CATEGORIES + " (" +
                CategoriesFields._id + " TEXT PRIMARY KEY, " +
                CategoriesFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                CategoriesFields.TYPE + " INTEGER DEFAULT 0 NOT NULL, " +
                CategoriesFields.PREFERRED_ACCOUNT + " TEXT DEFAULT NULL, " +
                " FOREIGN KEY (" + CategoriesFields.PREFERRED_ACCOUNT + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE SET NULL ON UPDATE CASCADE" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX CATEGORY_UNIQUE_NAME_IDX ON " + EntityType.CATEGORIES + " (" +  CategoriesFields.NAME + "," + CategoriesFields.TYPE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.BUDGETS + " (" +
                BudgetFields._id + " TEXT PRIMARY KEY, " +
                BudgetFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                BudgetFields.START_TIME + " TIMESTAMP NOT NULL, " +
                BudgetFields.END_TIME + " TIMESTAMP NOT NULL, " +
                BudgetFields.COVERED_ACCOUNT + " TEXT DEFAULT NULL, " +
                " FOREIGN KEY (" + BudgetFields.COVERED_ACCOUNT + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE SET NULL ON UPDATE CASCADE" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX BUDGET_UNIQUE_NAME_IDX ON " + EntityType.BUDGETS + " (" +  BudgetFields.NAME + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.BUDGET_ITEMS + " (" +
                BudgetItemFields._id + " TEXT PRIMARY KEY, " +
                BudgetItemFields.PARENT_BUDGET + " TEXT NOT NULL, " +
                BudgetItemFields.CATEGORY + " TEXT NOT NULL, " +
                BudgetItemFields.MAX_AMOUNT + " TEXT NOT NULL, " +
                " FOREIGN KEY (" + BudgetItemFields.CATEGORY + ") REFERENCES " + EntityType.CATEGORIES + " (" + CategoriesFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" +
                " FOREIGN KEY (" + BudgetItemFields.PARENT_BUDGET + ") REFERENCES " + EntityType.BUDGETS + " (" + BudgetFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX BUDGET_ITEM_UNIQUE_CATEGORY_PARENT_IDX ON " + EntityType.BUDGET_ITEMS + " (" +  BudgetItemFields.PARENT_BUDGET + "," + BudgetItemFields.CATEGORY + ")");


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

        Category outAdd = null;
        for(final String outCategory : defaultOutcomeCategories) {
            outAdd = new Category();
            outAdd.setName(outCategory);
            outAdd.setType(Category.EXPENSE);
            makeAction(ActionType.ADD, outAdd);
        }
        Category inAdd = null;
        for(final String inCategory : defaultIncomeCategories) {
            inAdd = new Category();
            inAdd.setName(inCategory);
            inAdd.setType(Category.INCOME);
            makeAction(ActionType.ADD, inAdd);
        }
        Category transAdd = null;
        for(final String transCategory : defaultTransCategories) {
            transAdd = new Category();
            transAdd.setName(transCategory);
            transAdd.setType(Category.TRANSFER);
            makeAction(ActionType.ADD, transAdd);
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
                        /* falls through */
                    case 2:
                        values.put(CurrenciesFields.DESCRIPTION.toString(), tokens[1]);
                        /* falls through */
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
        sqLiteDatabase.beginTransaction(); // initial fill
        final Random rand = new Random();
        for(int i = 0; i < 100; ++i) {
            final ContentValues accValues = new ContentValues(5);
            accValues.put(AccountFields._id.toString(), UUID.randomUUID().toString());
            accValues.put(AccountFields.NAME.toString(), "Account" + i);
            accValues.put(AccountFields.DESCRIPTION.toString(), "");
            accValues.put(AccountFields.CURRENCY.toString(), "RUB");
            accValues.put(AccountFields.AMOUNT.toString(), rand.nextInt(1000));
            accValues.put(AccountFields.COLOR.toString(), Color.rgb(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));

            for(int j = 0; j < 100; ++j) {
                final ContentValues opValues = new ContentValues(7);
                opValues.put(OperationsFields._id.toString(), UUID.randomUUID().toString());
                opValues.put(OperationsFields.DESCRIPTION.toString(), "operation" + j); // mandatory
                opValues.put(OperationsFields.CATEGORY.toString(), outAdd.getId()); // mandatory
                opValues.put(OperationsFields.AMOUNT.toString(), rand.nextInt(500)); // mandatory
                opValues.put(OperationsFields.CHARGER.toString(), accValues.getAsString(AccountFields._id.toString()));

                sqLiteDatabase.insert(EntityType.OPERATIONS.toString(), null, opValues);
            }

            sqLiteDatabase.insert(EntityType.ACCOUNTS.toString(), null, accValues);
        }
        sqLiteDatabase.setTransactionSuccessful(); // batch insert
        sqLiteDatabase.endTransaction();
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
        final int result = mDatabase.update(tableName, cv, "_id = ?", new String[]{cv.getAsString("_id")});
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

    public Cursor getAccountCursor() {
        Log.d("Database query", "getAccountCursor");
        return mDatabase.query(EntityType.ACCOUNTS.toString(), null, null, null, null, null, null, null);
    }

    public Cursor getCurrencyCursor() {
        Log.d("Database query", "getCurrencyCursor");
        return mDatabase.query(Currency.TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getOperationsCursor() {
        Log.d("Database query", "getOperationsCursor");
        return mDatabase.query(EntityType.OPERATIONS.toString(), null, null, null, null, null, OperationsFields.TIME + " ASC", null);
    }

    public Cursor getBudgetsCursor() {
        Log.d("Database query", "getBudgetsCursor");
        return mDatabase.query(EntityType.BUDGETS.toString(), null, null, null, null, null, null, null);
    }

    public BigDecimal getAmountForBudget(Budget budget, Category category) {
        final Cursor sumCounter;
        final BigDecimal sum;
        if(budget.getCoveredAccount() == null) // all accounts
            sumCounter = mDatabase.rawQuery("SELECT SUM(" + OperationsFields.AMOUNT + ") FROM " + EntityType.OPERATIONS + " WHERE " + OperationsFields.CATEGORY + " = ? AND " + OperationsFields.TIME + " BETWEEN ? AND ?", new String[]{category.getId(), String.valueOf(budget.getStartTime().getTime()), String.valueOf(budget.getEndTime().getTime())});
        else
            sumCounter = mDatabase.rawQuery("SELECT SUM(" + OperationsFields.AMOUNT + ") FROM " + EntityType.OPERATIONS + " WHERE " + OperationsFields.CHARGER + " = ? AND " + OperationsFields.CATEGORY + " = ? AND " + OperationsFields.TIME + " BETWEEN ? AND ?", new String[]{budget.getCoveredAccount().getId(), category.getId(), String.valueOf(budget.getStartTime().getTime()), String.valueOf(budget.getEndTime().getTime())});

        if(sumCounter.moveToFirst())
            sum = new BigDecimal(sumCounter.getLong(0));
        else
            sum = BigDecimal.ZERO;

        sumCounter.close();

        return sum;
    }

    public Cursor getEntityCursor(EntityType tableName, long id) {
        Log.d("Database query", "getOperationsCursor");
        return mDatabase.query(tableName.toString(), null, " _id = ?", new String[]{String.valueOf(id)}, null, null, null, null);
    }

    /**
     * Helper method to retrieve dependent entities names
     * Useful for spinner adapters
     *
     * @param tableName table to query foreign keys from
     * @param fkColumn  foreign key column in source table
     * @param targetTableName table name containing entities
     * @param nameColumn column representing name (for GUI needs) in target table
     * @return joined cursor with corresponding columns sourcetable.id <---> targettable.name
     */
    public Cursor getForeignNameCursor(EntityType tableName, String fkColumn, EntityType targetTableName, String nameColumn) {
        final SQLiteQueryBuilder filterBuilder = new SQLiteQueryBuilder();
        filterBuilder.setDistinct(true);
        filterBuilder.setTables(tableName + " AS t1" +
                " LEFT JOIN " + targetTableName + " AS t2 ON t1." + fkColumn + " = t2._id");
        return filterBuilder.query(mDatabase, new String[]{"t1." + fkColumn, "t2." + nameColumn}, null, null, null, null, null);
    }

    /**
     * Helper method to query entities by one column
     * Useful for cases when foreign key is not present in source table
     *
     * @param tableName table name where IDs reside
     * @param column column in table
     * @param value value of selected column
     * @return
     */
    public Cursor getCustomCursor(EntityType tableName, String column, String value) {
        return mDatabase.query(tableName.toString(), null, column + " = ?", new String[]{value}, null, null, null);
    }

    public Cursor getOperationsCursor(String filter) {
        Log.d("Database filter query", "getOperationsCursor with filter");
        final SQLiteQueryBuilder filterBuilder = new SQLiteQueryBuilder();

        final StringBuilder sb = new StringBuilder(20);
        sb.append("LOWER(");
        sb.append("COALESCE(op.").append(OperationsFields.DESCRIPTION).append(", '')");
        sb.append(" || ' ' || op.").append(OperationsFields.AMOUNT);
        sb.append(" || ' ' || strftime('%d.%m.%Y', op.").append(OperationsFields.TIME).append("/1000, 'unixepoch', 'localtime')");
        sb.append(" || ' ' || COALESCE(cats.").append(CategoriesFields.NAME).append(", '')");
        sb.append(" || ' ' || COALESCE(charger.").append(AccountFields.NAME).append(", '')");
        sb.append(" || ' ' || COALESCE(benefic.").append(AccountFields.NAME).append(", '')");

        sb.append(") LIKE LOWER(?)");
        filterBuilder.appendWhere(sb.toString());
        filterBuilder.setTables(EntityType.OPERATIONS + " AS op" +
                " LEFT JOIN " + EntityType.CATEGORIES + " AS cats " + "ON op." + OperationsFields.CATEGORY + " = " + "cats." + CategoriesFields._id +
                " LEFT JOIN " + EntityType.ACCOUNTS + " AS charger " + "ON op." + OperationsFields.CHARGER + " = " + "charger." + AccountFields._id +
                " LEFT JOIN " + EntityType.ACCOUNTS + " AS benefic " + "ON op." + OperationsFields.RECEIVER + " = " + "benefic." + AccountFields._id);
        return filterBuilder.query(mDatabase, new String[]{"op.*"}, null, new String[] {"%" + filter + "%"}, null, null, null);
    }

    public Cursor getCategoryCursor(int type) {
        Log.d("Database query", "getCategoryCursorWithType");
        return mDatabase.query(EntityType.CATEGORIES.toString(), null, CategoriesFields.TYPE + " = ?", new String[]{String.valueOf(type)}, null, null, CategoriesFields.NAME + " ASC", null);
    }

    @SuppressWarnings("unchecked")
    private void notifyListeners(String table) {
        // notify listeners that are subscribed to all events
        if(listenerMap.containsKey(DatabaseListener.ANY_TABLE)) {
            final ArrayList<DatabaseListener> shadow = (ArrayList<DatabaseListener>) listenerMap.get(DatabaseListener.ANY_TABLE).clone();
            for (final DatabaseListener listener : shadow)
                listener.handleUpdate();
        }

        if(listenerMap.containsKey(table)) {
            final ArrayList<DatabaseListener> shadow = (ArrayList<DatabaseListener>) listenerMap.get(table).clone();
            for (final DatabaseListener listener : shadow)
                listener.handleUpdate();
        }
    }

    public long addCurrency(Currency curr) {
        Log.d("Currency special", "addCurrency: " + curr);
        final ContentValues values = new ContentValues(3);
        values.put(CurrenciesFields.CODE.toString(), curr.getDescription());
        if(curr.getDescription() != null)
            values.put(CurrenciesFields.DESCRIPTION.toString(), curr.getDescription());
        if(curr.getUsedIn() != null)
            values.put(CurrenciesFields.USED_IN.toString(), curr.getUsedIn());

        return mDatabase.insert(Currency.TABLE_NAME, null, values);
    }

    public Currency getCurrency(String code) {
        Log.d("Currency special", "getCurrency: " + code);
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
                return Operation.getFromDB(id);
            }
        });
    }

    /**
     * Calling this method means we have full operation object with all data built and ready for applying
     * @param operation operation to be applied
     */
    public boolean applyOperation(Operation operation) {
        final Account chargeAcc = operation.getOrderer();
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
        return revertOperation(Operation.getFromDB(id));
    }

    /**
     * Calling this method means we have full operation object with all data built and ready for reverting
     * @param operation operation to be reverted
     */
    public boolean revertOperation(Operation operation) {
        final Account chargeAcc = operation.getOrderer();
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
                final Method filler = clazz.getDeclaredMethod("getFromDB", String.class);
                final T newEntity = (T) filler.invoke(null, selections.getString(0));
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
                final Method filler = clazz.getDeclaredMethod("getFromDB", String.class);
                final T newEntity = (T) filler.invoke(null, selections.getString(0));
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

