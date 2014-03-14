package com.adonai.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Operation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DatabaseDAO extends SQLiteOpenHelper
{
    /**
     * Calling this method means we have full operation object with all data built and ready for applying
     * @param operation operation to be applied
     */
    public boolean applyOperation(Operation operation) {
        final Account chargeAcc = operation.getCharger();
        final BigDecimal chargeAmount = operation.getAmountCharged();
        long successFlag = 0;
        boolean allSucceeded = false;
        mDatabase.beginTransaction();

        successFlag += addOperation(operation) > 0 ? 1 : 0;

        switch (operation.getOperationType()) {
            case TRANSFER:
                final Account beneficiarAcc = operation.getBeneficiar();
                final BigDecimal benAmount = operation.getConvertingRate() != null
                        ? chargeAmount.divide(BigDecimal.valueOf(operation.getConvertingRate()), 2, RoundingMode.HALF_UP)
                        : chargeAmount;

                beneficiarAcc.setAmount(beneficiarAcc.getAmount().add(benAmount));
                successFlag += updateAccount(beneficiarAcc); // apply to db

                chargeAcc.setAmount(chargeAcc.getAmount().subtract(chargeAmount));
                break;

            case EXPENSE: // subtract value
                chargeAcc.setAmount(chargeAcc.getAmount().subtract(chargeAmount));
                break;
            case INCOME: // add value
                chargeAcc.setAmount(chargeAcc.getAmount().add(chargeAmount));
                break;
        }
        successFlag += updateAccount(chargeAcc);

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
        final BigDecimal chargeAmount = operation.getAmountCharged();
        int successFlag = 0;
        boolean allSucceeded = false;
        mDatabase.beginTransaction();

        successFlag += deleteOperation(operation);

        switch (operation.getOperationType()) {
            case TRANSFER:
                final Account beneficiarAcc = operation.getBeneficiar();
                final BigDecimal benAmount = operation.getConvertingRate() != null
                        ? chargeAmount.divide(BigDecimal.valueOf(operation.getConvertingRate()), 2, RoundingMode.HALF_UP)
                        : chargeAmount;

                beneficiarAcc.setAmount(beneficiarAcc.getAmount().subtract(benAmount));
                successFlag += updateAccount(beneficiarAcc); // apply to db

                chargeAcc.setAmount(chargeAcc.getAmount().add(chargeAmount));
                break;

            case EXPENSE: // add subtracted value
                chargeAcc.setAmount(chargeAcc.getAmount().add(chargeAmount));
                break;
            case INCOME: // subtract added value
                chargeAcc.setAmount(chargeAcc.getAmount().subtract(chargeAmount));
                break;
        }
        successFlag += updateAccount(chargeAcc);

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
                OperationsFields.CHARGER + " INTEGER NOT NULL, " +
                OperationsFields.RECEIVER + " INTEGER DEFAULT NULL, " +
                OperationsFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
                OperationsFields.CONVERT_RATE + " REAL DEFAULT NULL, " +
                OperationsFields.GUID + " TEXT DEFAULT NULL, " +
                " FOREIGN KEY (" + OperationsFields.CATEGORY + ") REFERENCES " + CATEGORIES_TABLE_NAME + " (" + CategoriesFields._id + ") ON DELETE SET NULL," +
                " FOREIGN KEY (" + OperationsFields.CHARGER + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + AccountFields._id + ") ON DELETE CASCADE," + // delete associated transactions
                " FOREIGN KEY (" + OperationsFields.RECEIVER + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + AccountFields._id + ") ON DELETE CASCADE" + // delete associated transactions
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + CURRENCIES_TABLE_NAME + " (" +
                CurrenciesFields.CODE + " TEXT NOT NULL, " +
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public long addAccount(Account account) {
        Log.d("addAccount", account.getName());

        final ContentValues values = new ContentValues();
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
        Log.d("addOperation", operation.getAmountCharged().toPlainString());

        final ContentValues values = new ContentValues();
        values.put(OperationsFields.DESCRIPTION.toString(), operation.getDescription()); // mandatory
        values.put(OperationsFields.CATEGORY.toString(), operation.getCategory().getId()); // mandatory
        values.put(OperationsFields.AMOUNT.toString(), operation.getAmountCharged().toPlainString()); // mandatory

        if(operation.getTime() != null)
            values.put(OperationsFields.TIME.toString(), operation.getTimeString());
        values.put(OperationsFields.CHARGER.toString(), operation.getCharger().getId());
        if(operation.getBeneficiar() != null)
            values.put(OperationsFields.RECEIVER.toString(), operation.getBeneficiar().getId());
        values.put(OperationsFields.CONVERT_RATE.toString(), operation.getConvertingRate());

        long result = mDatabase.insert(OPERATIONS_TABLE_NAME, null, values);

        if(result != -1)
            notifyListeners(OPERATIONS_TABLE_NAME);

        return result;
    }

    public long addCategory(Category category) {
        Log.d("addCategory", category.getName());
        final ContentValues values = new ContentValues();
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
        final Cursor cursor = mDatabase.query(ACCOUNTS_TABLE_NAME, Utils.allKeys(AccountFields.class), " _id = ?", new String[] { String.valueOf(id) }, // d. selections args
                                 null, // e. group by
                                 null, // f. having
                                 null, // g. order by
                                 null); // h. limit

        if (cursor.moveToFirst()) {
            final Account acc = new Account();
            acc.setId(cursor.getLong(0));
            acc.setName(cursor.getString(1));
            acc.setDescription(cursor.getString(2));
            acc.setCurrency(Currency.getCurrencyForCode(cursor.getString(3)));
            acc.setAmount(new BigDecimal(cursor.getString(4)));
            acc.setColor(cursor.getInt(5));

            Log.d("getAccount(" + id + ")", acc.getName());
            cursor.close();
            return acc;
        }

        cursor.close();
        return null;
    }

    public Cursor getAccountCursor() {
        return mDatabase.query(ACCOUNTS_TABLE_NAME, Utils.allKeys(AccountFields.class), null, null, null, null, null, null);
    }

    public Cursor getOperationsCursor() {
        return mDatabase.query(OPERATIONS_TABLE_NAME, Utils.allKeys(OperationsFields.class), null, null, null, null, null, null);
    }

    public Cursor getCategoryCursor() {
        return mDatabase.query(CATEGORIES_TABLE_NAME, Utils.allKeys(CategoriesFields.class), null, null, null, null, null, null);
    }

    public Cursor getCategoryCursor(int type) {
        return mDatabase.query(CATEGORIES_TABLE_NAME, Utils.allKeys(CategoriesFields.class), CategoriesFields.TYPE + " = ?", new String[]{String.valueOf(type)}, null, null, null, null);
    }


    public boolean hasCategories() {
        final Cursor cur = getCategoryCursor();
        final boolean result = cur.moveToNext();
        cur.close();
        return result;
    }

    public Operation getOperation(long id) {
        final Cursor cursor = mDatabase.query(OPERATIONS_TABLE_NAME, Utils.allKeys(OperationsFields.class), OperationsFields._id + " = ?", new String[] {String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor.moveToFirst()) try {
            // 4. build operation object
            final Operation op = new Operation();
            op.setId(cursor.getLong(OperationsFields._id.ordinal()));
            op.setDescription(cursor.getString(OperationsFields.DESCRIPTION.ordinal()));
            op.setCategory(getCategory(cursor.getLong(OperationsFields.CATEGORY.ordinal())));
            op.setTime(Utils.SQLITE_DATE_FORMAT.parse(cursor.getString(OperationsFields.TIME.ordinal())));
            op.setCharger(getAccount(cursor.getInt(OperationsFields.CHARGER.ordinal())));
            if(!cursor.isNull(OperationsFields.RECEIVER.ordinal()))
                op.setBeneficiar(getAccount(cursor.getInt(OperationsFields.RECEIVER.ordinal())));
            op.setAmountCharged(new BigDecimal(cursor.getString(OperationsFields.AMOUNT.ordinal())));
            if(!cursor.isNull(OperationsFields.CONVERT_RATE.ordinal()))
                op.setConvertingRate(cursor.getDouble(OperationsFields.CONVERT_RATE.ordinal()));
            cursor.close();

            Log.d("getOperation(" + id + ")", op.getAmountCharged().toPlainString());
            return op;
        } catch (ParseException ex) {
            throw new RuntimeException(ex); // shouldn't happen
        }

        cursor.close();
        return null;
    }

    public Category getCategory(long id) {
        final Cursor cursor = mDatabase.query(CATEGORIES_TABLE_NAME, Utils.allKeys(CategoriesFields.class), CategoriesFields._id + " = ?", new String[] {String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor.moveToFirst()) {
            // 4. build operation object
            final Category cat = new Category();
            cat.setId(cursor.getLong(0));
            cat.setName(cursor.getString(1));
            cat.setType(cursor.getInt(2));
            if(!cursor.isNull(3))
                cat.setPreferredAccount(getAccount(cursor.getLong(3)));
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
        values.put(OperationsFields.AMOUNT.toString(), operation.getAmountCharged().toPlainString());
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
        Log.d("deleteOperation", operation.getAmountCharged().toPlainString());
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

    public long addCustomCurrency(Currency curr) {
        Currency.addCustomCurrency(curr);

        Log.d("addCurrency", curr.toString());
        final ContentValues values = new ContentValues();
        values.put(CurrenciesFields.CODE.toString(), curr.getDescription());
        if(curr.getDescription() != null)
            values.put(CurrenciesFields.DESCRIPTION.toString(), curr.getDescription());
        if(curr.getUsedIn() != null)
            values.put(CurrenciesFields.USED_IN.toString(), curr.getUsedIn());

        return mDatabase.insert(CURRENCIES_TABLE_NAME, null, values);
    }

    public List<Currency> getCustomCurrencies() {
        final Cursor cursor = mDatabase.query(CURRENCIES_TABLE_NAME, Utils.allKeys(CurrenciesFields.class), null, null, null,  null,  null, null);

        final List<Currency> result = new ArrayList<>();
        if (cursor.moveToFirst())
            while(cursor.moveToNext())
                result.add(new Currency(cursor.getString(0), cursor.getString(1), cursor.getString(2)));
        cursor.close();

        Log.d("getAllCurrencies", String.valueOf(result.size()));
        return result;
    }


}

