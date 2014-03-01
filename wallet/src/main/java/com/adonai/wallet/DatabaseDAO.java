package com.adonai.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Operation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DatabaseDAO extends SQLiteOpenHelper
{
    public interface DatabaseListener {
        void handleUpdate();
    }

    public void registerDatabaseListener(final String table, final DatabaseListener listener) {
        if(listenerMap.containsKey(table))
            listenerMap.get(table).add(listener);
        else
            listenerMap.put(table, new ArrayList<DatabaseListener>() {{ add(listener); }});
    }

    public static final String dbName = "moneyDB";
    public static final int dbVersion = 1;

    public static final String ACCOUNTS_TABLE_NAME = "accounts";
    private static final Map<String, String> accountCols = new LinkedHashMap<String, String>() // такая реализация не прерывает порядок очередности вставки
    {{
        put("ID", "_id");                       // 0
        put("name", "NAME");                    // 1
        put("description", "DESCRIPTION");      // 2
        put("currency", "CURRENCY");            // 3
        put("amount", "AMOUNT");                // 4
    }};

    public static final String OPERATIONS_TABLE_NAME = "operations";
    private static final Map<String, String> operationsCols = new LinkedHashMap<String, String>()
    {{
        put("ID", "_id");                       // 0
        put("description", "DESCRIPTION");      // 1
        put("time", "TIME");                    // 2
        put("charger", "CHARGER_ID");           // 3
        put("receiver", "RECEIVER_ID");         // 4
        put("amount", "AMOUNT");                // 5
        put("comission", "COMISSION");          // 6
    }};

    public static final String CURRENCIES_TABLE_NAME = "currencies";
    private static final Map<String, String> currenciesCols = new LinkedHashMap<String, String>()
    {{
        put("code", "CODE");                    // 0
        put("description", "DESCRIPTION");      // 1
        put("usedIn", "USED_IN");               // 2
    }};

    private final String[] accountKeys = accountCols.values().toArray(new String[accountCols.values().size()]);
    private final String[] operationsKeys = operationsCols.values().toArray(new String[operationsCols.values().size()]);
    private final String[] currenciesKeys = currenciesCols.values().toArray(new String[currenciesCols.values().size()]);
    private final Map<String, List<DatabaseListener>> listenerMap = new HashMap<>();
    private final SQLiteDatabase mDatabase;

    public DatabaseDAO(Context context) {
        super(context, dbName, null, dbVersion);
        mDatabase = getWritableDatabase();
        assert mDatabase != null;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
            super.onConfigure(db);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + ACCOUNTS_TABLE_NAME + " ("+
                accountKeys[0] + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                accountKeys[1] + " TEXT DEFAULT '' NOT NULL, " +
                accountKeys[2] + " TEXT DEFAULT NULL, " +
                accountKeys[3] + " TEXT DEFAULT 'RUB' NOT NULL, " +
                accountKeys[4] + " TEXT DEFAULT '0' NOT NULL " +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "ACCOUNT_NAME_IDX ON " + ACCOUNTS_TABLE_NAME + " (" + accountCols.get("name") + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + OPERATIONS_TABLE_NAME + " (" +
                operationsKeys[0] +" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                operationsKeys[1] +" TEXT DEFAULT NULL, " +
                operationsKeys[2] +" DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                operationsKeys[3] +" INTEGER NOT NULL, " +
                operationsKeys[4] +" INTEGER DEFAULT NULL, " +
                operationsKeys[5] +" TEXT DEFAULT '0', " +
                operationsKeys[6] +" REAL NOT NULL, " +
                " FOREIGN KEY (" + operationsKeys[3] + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + accountKeys[0] + ")," +
                " FOREIGN KEY (" + operationsKeys[4] + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + accountKeys[0] + ")" +
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + CURRENCIES_TABLE_NAME + " (" +
                currenciesKeys[0] +" TEXT NOT NULL, " +
                currenciesKeys[1] +" TEXT DEFAULT NULL, " +
                currenciesKeys[2] +" TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CURRENCY_IDX ON " + CURRENCIES_TABLE_NAME + " (" +  currenciesCols.get("code") + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public long addAccount(Account account) {
        Log.d("addAccount", account.getName());

        final ContentValues values = new ContentValues();
        values.put(accountCols.get("name"), account.getName());
        values.put(accountCols.get("description"), account.getDescription());
        values.put(accountCols.get("currency"), account.getCurrency().toString());
        values.put(accountCols.get("amount"), account.getAmount().toString());

        long result = mDatabase.insert(ACCOUNTS_TABLE_NAME, null, values);

        if(result != -1 && listenerMap.containsKey(ACCOUNTS_TABLE_NAME))
            for(final DatabaseListener listener : listenerMap.get(ACCOUNTS_TABLE_NAME))
                listener.handleUpdate();

        return result;
    }

    public long addOperation(Operation operation) {
        Log.d("addOperation", operation.getAmountCharged().toPlainString());

        final ContentValues values = new ContentValues();
        values.put(operationsCols.get("description"), operation.getDescription());
        if(operation.getTime() != null)
            values.put(operationsCols.get("time"), operation.getTimeString());
        values.put(operationsCols.get("charger"), operation.getCharger().getId());
        if(operation.getReceiver() != null)
            values.put(operationsCols.get("receiver"), operation.getReceiver().getId());
        values.put(operationsCols.get("amount"), operation.getAmountCharged().toString());
        values.put(operationsCols.get("comission"), operation.getConvertingComission());

        long result = mDatabase.insert(OPERATIONS_TABLE_NAME, null, values);

        if(result != -1 && listenerMap.containsKey(OPERATIONS_TABLE_NAME))
            for(final DatabaseListener listener : listenerMap.get(OPERATIONS_TABLE_NAME))
                listener.handleUpdate();

        return result;
    }

    public Account getAccount(long id) {
        final Cursor cursor = mDatabase.query(ACCOUNTS_TABLE_NAME, accountKeys, " id = ?", new String[] { String.valueOf(id) }, // d. selections args
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
            acc.setAmount(new BigDecimal(cursor.getString(2)));

            Log.d("getAccount(" + id + ")", acc.getName());
            cursor.close();
            return acc;
        }

        cursor.close();
        return null;
    }

    public Cursor getAcountCursor() {
        return mDatabase.query(ACCOUNTS_TABLE_NAME, accountKeys, null, null, null, null, null, null);
    }

    public Operation getOperaion(long id) {
        final Cursor cursor = mDatabase.query(OPERATIONS_TABLE_NAME, operationsKeys, " id = ?", new String[] { String.valueOf(id) }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor.moveToFirst()) {
            // 4. build operation object
            final Operation op = new Operation();
            op.setId(cursor.getLong(0));
            op.setDescription(cursor.getString(1));
            op.setTime(new Date(cursor.getLong(2)));
            op.setCharger(getAccount(cursor.getInt(3)));
            if(!cursor.isNull(4))
                op.setReceiver(getAccount(cursor.getInt(4)));
            op.setAmountCharged(new BigDecimal(cursor.getString(5)));
            if(!cursor.isNull(6))
                op.setConvertingComission(cursor.getDouble(6));
            cursor.close();

            Log.d("getOperation(" + id + ")", op.getAmountCharged().toPlainString());
            return op;
        }

        cursor.close();
        return null;
    }

    public int updateAccount(Account account) {
        final ContentValues values = new ContentValues();
        values.put(accountCols.get("name"), account.getName());
        values.put(accountCols.get("description"), account.getDescription());
        values.put(accountCols.get("currency"), account.getCurrency().toString());
        values.put(accountCols.get("amount"), account.getAmount().toString());

        return mDatabase.update(ACCOUNTS_TABLE_NAME, //table
                values, // column/value
                accountCols.get("ID") + " = ?", // selections
                new String[] { String.valueOf(account.getId()) });
    }

    public int updateOperation(Operation operation) {
        // 2. create ContentValues to add key "column"/value
        final ContentValues values = new ContentValues();
        values.put(operationsCols.get("description"), operation.getDescription());
        if(operation.getTime() != null)
            values.put(operationsCols.get("time"), operation.getTimeString());
        values.put(operationsCols.get("charger"), operation.getCharger().getId());
        if(operation.getReceiver() != null)
            values.put(operationsCols.get("receiver"), operation.getReceiver().getId());
        values.put(operationsCols.get("amount"), operation.getAmountCharged().toString());
        values.put(operationsCols.get("comission"), operation.getConvertingComission());

        return mDatabase.update(ACCOUNTS_TABLE_NAME, //table
                values, // column/value
                operationsCols.get("ID") + " = ?", // selections
                new String[] { String.valueOf(operation.getId()) }); //selection args
    }

    public int deleteAccount(Account account) {
        Log.d("deleteAccount", account.getName());

        return mDatabase.delete(ACCOUNTS_TABLE_NAME, //table name
                accountCols.get("ID") + " = ?",  // selections
                new String[] { String.valueOf(account.getId()) });
    }

    public int deleteOperation(Operation operation) {
        Log.d("deleteOperation", operation.getAmountCharged().toPlainString());
        return mDatabase.delete(OPERATIONS_TABLE_NAME, //table name
                operationsCols.get("ID") + " = ?",  // selections
                new String[] { String.valueOf(operation.getId()) });
    }

    public long addCustomCurrency(Currency curr) {
        Currency.addCustomCurrency(curr);

        Log.d("addCurrency", curr.toString());
        final ContentValues values = new ContentValues();
        values.put(currenciesCols.get("code"), curr.getDescription());
        if(curr.getDescription() != null)
            values.put(currenciesCols.get("description"), curr.getDescription());
        if(curr.getUsedIn() != null)
            values.put(currenciesCols.get("usedIn"), curr.getUsedIn());

        return mDatabase.insert(CURRENCIES_TABLE_NAME, null, values);
    }

    public List<Currency> getCustomCurrencies() {
        final Cursor cursor = mDatabase.query(CURRENCIES_TABLE_NAME, currenciesKeys, null, null, null,  null,  null, null);

        final List<Currency> result = new ArrayList<>();
        if (cursor.moveToFirst())
            while(cursor.moveToNext())
                result.add(new Currency(cursor.getString(0), cursor.getString(1), cursor.getString(2)));
        cursor.close();

        Log.d("getAllCurrencies", String.valueOf(result.size()));
        return result;
    }


}

