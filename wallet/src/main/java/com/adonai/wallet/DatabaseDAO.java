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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DatabaseDAO extends SQLiteOpenHelper
{
    static final String dbName = "moneyDB";
    static final int dbVersion = 1;

    private static final String accountTable = "accounts";
    private static final Map<String, String> accountCols = new LinkedHashMap<String, String>() // такая реализация не прерывает порядок очередности вставки
    {{
            put("ID", "_id");                       // 0
            put("name", "NAME");                    // 1
            put("description", "DESCRIPTION");      // 2
            put("currency", "CURRENCY");            // 3
            put("amount", "AMOUNT");                // 4
        }};

    private static final String operationsTable = "operations";
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

    private static final String currenciesTable = "currencies";
    private static final Map<String, String> currenciesCols = new LinkedHashMap<String, String>()
    {{
            put("code", "CODE");                    // 0
            put("description", "DESCRIPTION");      // 1
            put("usedIn", "USED_IN");               // 2
        }};

    private String[] accountKeys = accountCols.values().toArray(new String[accountCols.values().size()]);
    private String[] operationsKeys = operationsCols.values().toArray(new String[operationsCols.values().size()]);
    private String[] currenciesKeys = currenciesCols.values().toArray(new String[currenciesCols.values().size()]);


    public DatabaseDAO(Context context) {
        super(context, dbName, null, dbVersion);
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
        sqLiteDatabase.execSQL("CREATE TABLE " + accountTable + " ("+
                accountKeys[0] + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                accountKeys[1] + " TEXT DEFAULT '' NOT NULL, " +
                accountKeys[2] + " TEXT DEFAULT NULL, " +
                accountKeys[3] + " TEXT DEFAULT 'RUB' NOT NULL, " +
                accountKeys[4] + " TEXT DEFAULT '0' NOT NULL " +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "ACCOUNT_NAME_IDX ON " + accountTable + " (" + accountCols.get("name") + ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + operationsTable + " (" +
                operationsKeys[0] +" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                operationsKeys[1] +" TEXT DEFAULT NULL, " +
                operationsKeys[2] +" DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                operationsKeys[3] +" INTEGER NOT NULL, " +
                operationsKeys[4] +" INTEGER DEFAULT NULL, " +
                operationsKeys[5] +" TEXT DEFAULT '0', " +
                operationsKeys[6] +" REAL NOT NULL, " +
                " FOREIGN KEY (" + operationsKeys[3] + ") REFERENCES " + accountTable + " (" + accountKeys[0] + ")," +
                " FOREIGN KEY (" + operationsKeys[4] + ") REFERENCES " + accountTable + " (" + accountKeys[0] + ")" +
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + currenciesTable + " (" +
                currenciesKeys[0] +" TEXT NOT NULL, " +
                currenciesKeys[1] +" TEXT DEFAULT NULL, " +
                currenciesKeys[2] +" TEXT DEFAULT NULL" +
                ")");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "CURRENCY_IDX ON " + currenciesTable + " (" +  currenciesCols.get("code") + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public long addAccount(Account account) {
        //for logging
        Log.d("addAccount", account.getName());

        // 1. get reference to writable DB
        final SQLiteDatabase db = getWritableDatabase();
        assert db != null;

        // 2. create ContentValues to add key "column"/value
        final ContentValues values = new ContentValues();
        values.put(accountCols.get("name"), account.getName());
        values.put(accountCols.get("description"), account.getDescription());
        values.put(accountCols.get("currency"), account.getCurrency().toString());
        values.put(accountCols.get("amount"), account.getAmount().toString());

        // 3. insert
        long result = db.insert(accountTable, null, values);
        db.close();

        return result;
    }

    public long addOperation(Operation operation) {
        //for logging
        Log.d("addOperation", operation.getAmountCharged().toPlainString());

        // 1. get reference to writable DB
        final SQLiteDatabase db = getWritableDatabase();
        assert db != null;

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

        // 3. insert
        long result = db.insert(operationsTable, null, values);
        db.close();

        return result;
    }

    public Account getAccount(long id) {
        // 1. get reference to readable DB
        final SQLiteDatabase db = getReadableDatabase();
        assert db != null;

        // 2. build query
        final Cursor cursor = db.query(accountTable, accountKeys, " id = ?", new String[] { String.valueOf(id) }, // d. selections args
                                 null, // e. group by
                                 null, // f. having
                                 null, // g. order by
                                 null); // h. limit

        // 3. if we got results get the first one
        if (cursor.moveToFirst()) {
            // 4. build book object
            Account acc = new Account();
            acc.setId(cursor.getLong(0));
            acc.setName(cursor.getString(1));
            acc.setDescription(cursor.getString(2));
            acc.setCurrency(Currency.getCurrencyForCode(cursor.getString(3)));
            acc.setAmount(new BigDecimal(cursor.getString(2)));

            //log
            Log.d("getAccount(" + id + ")", acc.getName());
            return acc;
        }

        return null;
    }

    public Operation getOperaion(long id) {
        // 1. get reference to readable DB
        final SQLiteDatabase db = getReadableDatabase();
        assert db != null;

        // 2. build query
        final Cursor cursor = db.query(operationsTable, operationsKeys, " id = ?", new String[] { String.valueOf(id) }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        // 3. if we got results get the first one
        if (cursor.moveToFirst()) {
            // 4. build operation object
            Operation op = new Operation();
            op.setId(cursor.getLong(0));
            op.setDescription(cursor.getString(1));
            op.setTime(new Date(cursor.getLong(2)));
            op.setCharger(getAccount(cursor.getInt(3)));
            if(!cursor.isNull(4))
                op.setReceiver(getAccount(cursor.getInt(4)));
            op.setAmountCharged(new BigDecimal(cursor.getString(5)));
            if(!cursor.isNull(6))
                op.setConvertingComission(cursor.getDouble(6));

            //log
            Log.d("getOperation(" + id + ")", op.getAmountCharged().toPlainString());
            return op;
        }

        return null;
    }

    public int updateAccount(Account account) {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        // 2. create ContentValues to add key "column"/value
        final ContentValues values = new ContentValues();
        values.put(accountCols.get("name"), account.getName());
        values.put(accountCols.get("description"), account.getDescription());
        values.put(accountCols.get("currency"), account.getCurrency().toString());
        values.put(accountCols.get("amount"), account.getAmount().toString());

        // 3. updating row
        int i = db.update(accountTable, //table
                values, // column/value
                accountCols.get("ID") + " = ?", // selections
                new String[] { String.valueOf(account.getId()) }); //selection args

        // 4. close
        db.close();

        return i;
    }

    public int updateOperation(Operation operation) {
        // 1. get reference to writable DB
        final SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

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

        // 3. updating row
        int i = db.update(accountTable, //table
                values, // column/value
                operationsCols.get("ID") + " = ?", // selections
                new String[] { String.valueOf(operation.getId()) }); //selection args

        // 4. close
        db.close();

        return i;
    }

    public int deleteAccount(Account account) {
        // 1. get reference to writable DB
        final SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        // 2. delete
        int result = db.delete(accountTable, //table name
                accountCols.get("ID") + " = ?",  // selections
                new String[] { String.valueOf(account.getId()) }); //selections args
        db.close();

        //log
        Log.d("deleteAccount", account.getName());

        return result;
    }

    public int deleteOperation(Operation operation) {
        // 1. get reference to writable DB
        final SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        int result = db.delete(operationsTable, //table name
                operationsCols.get("ID") + " = ?",  // selections
                new String[] { String.valueOf(operation.getId()) }); //selections args
        db.close();

        Log.d("deleteOperation", operation.getAmountCharged().toPlainString());

        return result;
    }

    public long addCustomCurrency(Currency curr) {
        Currency.addCustomCurrency(curr);

        final SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        final ContentValues values = new ContentValues();
        values.put(currenciesCols.get("code"), curr.getDescription());
        if(curr.getDescription() != null)
            values.put(currenciesCols.get("description"), curr.getDescription());
        if(curr.getUsedIn() != null)
            values.put(currenciesCols.get("usedIn"), curr.getUsedIn());

        // 3. insert
        long result = db.insert(currenciesTable, null, values);
        db.close();

        Log.d("addCurrency", curr.toString());

        return result;
    }

    public List<Currency> getCustomCurrencies() {
        final SQLiteDatabase db = getReadableDatabase();
        assert db != null;

        // 2. build query
        final Cursor cursor = db.query(currenciesTable, currenciesKeys, null, null, null,  null,  null, null);

        // 3. if we got results get the first one
        final List<Currency> result = new ArrayList<>();
        if (cursor.moveToFirst())
            while(cursor.moveToNext())
            result.add(new Currency(cursor.getString(0), cursor.getString(1), cursor.getString(2)));


        //log
        Log.d("getAllCurrencies", String.valueOf(result.size()));

        return result;
    }
}

