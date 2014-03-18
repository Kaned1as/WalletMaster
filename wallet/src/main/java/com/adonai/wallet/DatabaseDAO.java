package com.adonai.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Operation;

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
import java.util.concurrent.Callable;


public class DatabaseDAO extends SQLiteOpenHelper
{
    private final Context mContext;
    /**
     * Calling this method means we have full operation object with all data built and ready for applying
     * @param operation operation to be applied
     */
    public boolean applyOperation(Operation operation) {
        final Account chargeAcc = operation.getCharger();
        final Account benefAcc = operation.getBeneficiar();
        final BigDecimal amount = operation.getAmount();

        long successFlag = 0;
        boolean allSucceeded = false;
        mDatabase.beginTransaction();

        successFlag += addOperation(operation) > 0 ? 1 : 0;

        switch (operation.getOperationType()) {
            case TRANSFER:
                benefAcc.setAmount(benefAcc.getAmount().add(operation.getAmountDelivered()));
                chargeAcc.setAmount(chargeAcc.getAmount().subtract(amount));
                successFlag += updateAccount(benefAcc); // apply to db
                successFlag += updateAccount(chargeAcc);
                break;
            case EXPENSE: // subtract value
                chargeAcc.setAmount(chargeAcc.getAmount().subtract(amount));
                successFlag += updateAccount(chargeAcc);
                break;
            case INCOME: // add value
                benefAcc.setAmount(benefAcc.getAmount().add(amount));
                successFlag += updateAccount(benefAcc);
                break;
        }
                                            // transfer complete                                        // income/expense complete
        if(operation.getOperationType() == Operation.OperationType.TRANSFER && successFlag == 3 || operation.getOperationType() != Operation.OperationType.TRANSFER && successFlag == 2) {
            mDatabase.setTransactionSuccessful();
            allSucceeded = true;

            notifyListeners(OPERATIONS_TABLE_NAME);
            notifyListeners(ACCOUNTS_TABLE_NAME);
        }

        mDatabase.endTransaction();
        return allSucceeded;
    }

    /**
     * Calling this method means we have full operation object with all data built and ready for reverting
     * @param operation operation to be reverted
     */
    public boolean revertOperation(Operation operation) {
        final Account chargeAcc = operation.getCharger();
        final Account benefAcc = operation.getBeneficiar();
        final BigDecimal amount = operation.getAmount();

        long successFlag = 0;
        boolean allSucceeded = false;
        mDatabase.beginTransaction();

        successFlag += deleteOperation(operation);

        switch (operation.getOperationType()) {
            case TRANSFER:
                benefAcc.setAmount(benefAcc.getAmount().subtract(operation.getAmountDelivered()));
                chargeAcc.setAmount(chargeAcc.getAmount().add(amount));
                successFlag += updateAccount(benefAcc); // apply to db
                successFlag += updateAccount(chargeAcc);
                break;
            case EXPENSE: // add subtracted value
                chargeAcc.setAmount(chargeAcc.getAmount().add(amount));
                successFlag += updateAccount(chargeAcc);
                break;
            case INCOME: // subtract added value
                benefAcc.setAmount(benefAcc.getAmount().subtract(amount));
                successFlag += updateAccount(benefAcc);
                break;
        }
                                            // transfer complete                                        // income/expense complete
        if(operation.getOperationType() == Operation.OperationType.TRANSFER && successFlag == 3 || operation.getOperationType() != Operation.OperationType.TRANSFER && successFlag == 2) {
            mDatabase.setTransactionSuccessful();
            allSucceeded = true;

            notifyListeners(OPERATIONS_TABLE_NAME);
            notifyListeners(ACCOUNTS_TABLE_NAME);
        }

        mDatabase.endTransaction();
        return allSucceeded;
    }

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

    public static final String ACCOUNTS_TABLE_NAME = "accounts";
    public static enum AccountFields {
        _id,
        NAME,
        DESCRIPTION,
        CURRENCY,
        AMOUNT,
        COLOR,
        GUID
    }

    public static final String OPERATIONS_TABLE_NAME = "operations";
    public static enum OperationsFields {
        _id,
        DESCRIPTION,
        CATEGORY,
        TIME,
        CHARGER,
        RECEIVER,
        AMOUNT,
        CONVERT_RATE,
        GUID
    }

    public static final String CURRENCIES_TABLE_NAME = "currencies";
    public static enum CurrenciesFields {
        CODE,
        DESCRIPTION,
        USED_IN,
    }


    public static final String CATEGORIES_TABLE_NAME = "categories";
    public static enum CategoriesFields {
        _id,
        NAME,
        TYPE,
        PREFERRED_ACCOUNT,
        GUID
    }

    private final Map<String, List<DatabaseListener>> listenerMap = new HashMap<>();
    private final SQLiteDatabase mDatabase;

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
        sqLiteDatabase.execSQL("CREATE TABLE " + ACCOUNTS_TABLE_NAME + " (" +
                AccountFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                AccountFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                AccountFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                AccountFields.CURRENCY + " TEXT DEFAULT 'RUB' NOT NULL, " +
                AccountFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                AccountFields.COLOR + " TEXT DEFAULT NULL, " +
                AccountFields.GUID + " TEXT DEFAULT NULL " +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "ACCOUNT_NAME_IDX ON " + ACCOUNTS_TABLE_NAME + " (" + AccountFields.NAME + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + OPERATIONS_TABLE_NAME + " (" +
                OperationsFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                OperationsFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                OperationsFields.CATEGORY + " INTEGER NOT NULL, " +
                OperationsFields.TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                OperationsFields.CHARGER + " INTEGER DEFAULT NULL, " +
                OperationsFields.RECEIVER + " INTEGER DEFAULT NULL, " +
                OperationsFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                OperationsFields.CONVERT_RATE + " REAL DEFAULT NULL, " +
                OperationsFields.GUID + " TEXT DEFAULT NULL, " +
                " FOREIGN KEY (" + OperationsFields.CATEGORY + ") REFERENCES " + CATEGORIES_TABLE_NAME + " (" + CategoriesFields._id + ") ON DELETE SET NULL," +
                " FOREIGN KEY (" + OperationsFields.CHARGER + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + AccountFields._id + ") ON DELETE CASCADE," + // delete associated transactions
                " FOREIGN KEY (" + OperationsFields.RECEIVER + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + AccountFields._id + ") ON DELETE CASCADE" + // delete associated transactions
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + CURRENCIES_TABLE_NAME + " (" +
                CurrenciesFields.CODE + " TEXT PRIMARY KEY NOT NULL, " +
                CurrenciesFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
                CurrenciesFields.USED_IN + " TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CURRENCY_IDX ON " + CURRENCIES_TABLE_NAME + " (" +  CurrenciesFields.CODE + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + CATEGORIES_TABLE_NAME + " (" +
                CategoriesFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CategoriesFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
                CategoriesFields.TYPE + " INTEGER DEFAULT 0 NOT NULL, " +
                CategoriesFields.PREFERRED_ACCOUNT + " INTEGER DEFAULT NULL, " +
                CategoriesFields.GUID + " TEXT DEFAULT NULL, " +
                " FOREIGN KEY (" + CategoriesFields.PREFERRED_ACCOUNT + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + AccountFields._id + ") ON DELETE SET NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CATEGORY_UNIQUE_NAME_IDX ON " + CATEGORIES_TABLE_NAME + " (" +  CategoriesFields.NAME + ")");

        sqLiteDatabase.beginTransaction(); // initial fill
        // fill Categories
        final String[] defaultOutcomeCategories = mContext.getResources().getStringArray(R.array.out_categories);
        final String[] defaultIncomeCategories = mContext.getResources().getStringArray(R.array.inc_categories);
        for(final String outCategory : defaultOutcomeCategories) {
            final ContentValues values = new ContentValues(2);
            values.put(CategoriesFields.NAME.toString(), outCategory);
            values.put(CategoriesFields.TYPE.toString(), Category.EXPENSE);
            sqLiteDatabase.insert(CATEGORIES_TABLE_NAME, null, values);
        }
        for(final String inCategory : defaultIncomeCategories) {
            final ContentValues values = new ContentValues(2);
            values.put(CategoriesFields.NAME.toString(), inCategory);
            values.put(CategoriesFields.TYPE.toString(), Category.INCOME);
            sqLiteDatabase.insert(CATEGORIES_TABLE_NAME, null, values);
        }

        //fill Currencies
        final InputStream allCurrencies = getClass().getResourceAsStream("/assets/currencies.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(allCurrencies));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                final String[] tokens = line.split(":");
                final ContentValues values = new ContentValues(3);
                switch (tokens.length) { // case-no-break magic!
                    case 3:
                        values.put(CurrenciesFields.USED_IN.toString(), tokens[2]);
                    case 2:
                        values.put(CurrenciesFields.DESCRIPTION.toString(), tokens[1]);
                    case 1:
                        values.put(CurrenciesFields.CODE.toString(), tokens[0]);
                        break;
                }
                sqLiteDatabase.insert(CURRENCIES_TABLE_NAME, null, values);
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // should not happen!
        }

        sqLiteDatabase.setTransactionSuccessful(); // batch insert
        sqLiteDatabase.endTransaction();
        // fill
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public long addAccount(Account account) {
        Log.d("addAccount", account.getName());

        final ContentValues values = new ContentValues(5);
        values.put(AccountFields.NAME.toString(), account.getName());
        values.put(AccountFields.DESCRIPTION.toString(), account.getDescription());
        values.put(AccountFields.CURRENCY.toString(), account.getCurrency().toString());
        values.put(AccountFields.AMOUNT.toString(), account.getAmount().toPlainString());
        values.put(AccountFields.COLOR.toString(), account.getColor());

        long result = mDatabase.insert(ACCOUNTS_TABLE_NAME, null, values);

        if(result != -1)
            notifyListeners(ACCOUNTS_TABLE_NAME);

        return result;
    }

    public long addOperation(Operation operation) {
        Log.d("addOperation", operation.getAmount().toPlainString());

        final ContentValues values = new ContentValues(7);
        values.put(OperationsFields.DESCRIPTION.toString(), operation.getDescription()); // mandatory
        values.put(OperationsFields.CATEGORY.toString(), operation.getCategory().getId()); // mandatory
        values.put(OperationsFields.AMOUNT.toString(), operation.getAmount().toPlainString()); // mandatory

        if(operation.getTime() != null)
            values.put(OperationsFields.TIME.toString(), operation.getTimeString());
        if(operation.getCharger() != null)
            values.put(OperationsFields.CHARGER.toString(), operation.getCharger().getId());
        if(operation.getBeneficiar() != null)
            values.put(OperationsFields.RECEIVER.toString(), operation.getBeneficiar().getId());
        if(operation.getConvertingRate() != null)
            values.put(OperationsFields.CONVERT_RATE.toString(), operation.getConvertingRate());

        long result = mDatabase.insert(OPERATIONS_TABLE_NAME, null, values);

        if(result != -1)
            notifyListeners(OPERATIONS_TABLE_NAME);

        return result;
    }

    public long addCategory(Category category) {
        Log.d("addCategory", category.getName());
        final ContentValues values = new ContentValues(3);
        values.put(CategoriesFields.NAME.toString(), category.getName());
        values.put(CategoriesFields.TYPE.toString(), category.getType());
        if(category.getPreferredAccount() != null)
            values.put(CategoriesFields.PREFERRED_ACCOUNT.toString(), category.getPreferredAccount().getId());

        long result = mDatabase.insert(CATEGORIES_TABLE_NAME, null, values);

        if(result != -1)
            notifyListeners(CATEGORIES_TABLE_NAME);

        return result;
    }

    public Account getAccount(long id) {
        final Cursor cursor = mDatabase.query(ACCOUNTS_TABLE_NAME, null, " _id = ?", new String[] { String.valueOf(id) }, // d. selections args
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
        return mDatabase.query(ACCOUNTS_TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getCurrencyCursor() {
        Log.d("Query", "getCurrencyCursor");
        return mDatabase.query(CURRENCIES_TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getOperationsCursor() {
        Log.d("Query", "getOperationsCursor");
        return mDatabase.query(OPERATIONS_TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getCategoryCursor() {
        Log.d("Query", "getCategoryCursor");
        return mDatabase.query(CATEGORIES_TABLE_NAME, null, null, null, null, null, null, null);
    }

    public Cursor getCategoryCursor(int type) {
        Log.d("Query", "getCategoryCursorWithType");
        return mDatabase.query(CATEGORIES_TABLE_NAME, null, CategoriesFields.TYPE + " = ?", new String[]{String.valueOf(type)}, null, null, null, null);
    }

    public Operation getOperation(long id) {
        final Long preciseTime = System.currentTimeMillis();
        final Cursor cursor = mDatabase.query(OPERATIONS_TABLE_NAME, null, OperationsFields._id + " = ?", new String[] {String.valueOf(id)}, // d. selections args
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
            throw new RuntimeException(ex); // shouldn't happen
        }

        cursor.close();
        return null;
    }

    public Category getCategory(long id) {
        final Cursor cursor = mDatabase.query(CATEGORIES_TABLE_NAME, null, CategoriesFields._id + " = ?", new String[] {String.valueOf(id)}, // d. selections args
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

    public int updateAccount(Account account) {
        final ContentValues values = new ContentValues();
        values.put(AccountFields.NAME.toString(), account.getName());
        values.put(AccountFields.DESCRIPTION.toString(), account.getDescription());
        values.put(AccountFields.CURRENCY.toString(), account.getCurrency().toString());
        values.put(AccountFields.AMOUNT.toString(), account.getAmount().toPlainString());
        values.put(AccountFields.COLOR.toString(), account.getColor());

        final int result = mDatabase.update(ACCOUNTS_TABLE_NAME,  values,  AccountFields._id + " = ?",  new String[] { String.valueOf(account.getId()) });

        if(result > 0)
            notifyListeners(ACCOUNTS_TABLE_NAME);

        return result;
    }

    public int updateOperation(Operation operation) {
        // 2. create ContentValues to add key "column"/value
        final ContentValues values = new ContentValues();
        values.put(OperationsFields.DESCRIPTION.toString(), operation.getDescription());
        if(operation.getTime() != null)
            values.put(OperationsFields.TIME.toString(), operation.getTimeString());
        values.put(OperationsFields.CHARGER.toString(), operation.getCharger().getId());
        if(operation.getBeneficiar() != null)
            values.put(OperationsFields.RECEIVER.toString(), operation.getBeneficiar().getId());
        values.put(OperationsFields.AMOUNT.toString(), operation.getAmount().toPlainString());
        values.put(OperationsFields.CONVERT_RATE.toString(), operation.getConvertingRate());

        final int result = mDatabase.update(OPERATIONS_TABLE_NAME,  values, OperationsFields._id + " = ?",  new String[] { String.valueOf(operation.getId()) }); //selection args

        if(result > 0)
            notifyListeners(OPERATIONS_TABLE_NAME);

        return result;
    }

    public int deleteAccount(Account account) {
        Log.d("deleteAccount", account.getName());

        int count = mDatabase.delete(ACCOUNTS_TABLE_NAME, //table name
                AccountFields._id + " = ?",  // selections
                new String[]{String.valueOf(account.getId())});

        if(count > 0)
            notifyListeners(ACCOUNTS_TABLE_NAME);

        return count;
    }

    public int deleteAccount(final Long id) {
        Log.d("deleteAccount", String.valueOf(id));

        int count = mDatabase.delete(ACCOUNTS_TABLE_NAME, AccountFields._id + " = ?", new String[] { String.valueOf(id) });

        if(count > 0)
            notifyListeners(ACCOUNTS_TABLE_NAME);

        return count;
    }

    public int deleteCategory(final Long id) {
        Log.d("deleteCategory", String.valueOf(id));

        int count = mDatabase.delete(CATEGORIES_TABLE_NAME, CategoriesFields._id + " = ?", new String[] { String.valueOf(id) });

        if(count > 0)
            notifyListeners(CATEGORIES_TABLE_NAME);

        return count;
    }

    public int deleteOperation(Operation operation) {
        Log.d("deleteOperation", operation.getId().toString());
        int count = mDatabase.delete(OPERATIONS_TABLE_NAME, //table name
                OperationsFields._id + " = ?",  // selections
                new String[] { String.valueOf(operation.getId()) });

        if(count > 0)
            notifyListeners(OPERATIONS_TABLE_NAME);

        return count;
    }

    public int deleteOperation(Long id) {
        Log.d("deleteOperation", id.toString());
        int count = mDatabase.delete(OPERATIONS_TABLE_NAME,  OperationsFields._id + " = ?", new String[] { String.valueOf(id) });

        if(count > 0)
            notifyListeners(OPERATIONS_TABLE_NAME);

        return count;
    }

    public long addCurrency(Currency curr) {
        Log.d("addCurrency", curr.toString());
        final ContentValues values = new ContentValues(3);
        values.put(CurrenciesFields.CODE.toString(), curr.getDescription());
        if(curr.getDescription() != null)
            values.put(CurrenciesFields.DESCRIPTION.toString(), curr.getDescription());
        if(curr.getUsedIn() != null)
            values.put(CurrenciesFields.USED_IN.toString(), curr.getUsedIn());

        return mDatabase.insert(CURRENCIES_TABLE_NAME, null, values);
    }

    public Currency getCurrency(String code) {
        Log.d("getCurrency", code);
        final Cursor cursor = mDatabase.query(CURRENCIES_TABLE_NAME, null, CurrenciesFields.CODE + " = ?", new String[]{code}, null,  null,  null, null);

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
}

