package com.adonai.wallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Operation;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
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

    private static final String operationsTable="operations";
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

    private static String[] accountKeys = accountCols.values().toArray(new String[accountCols.values().size()]);
    private String[] operationsKeys = operationsCols.values().toArray(new String[operationsCols.values().size()]);


    public DatabaseDAO(Context context) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + accountTable + " ("+
                accountKeys[0] + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                accountKeys[1] + " TEXT DEFAULT '' NOT NULL, " +
                accountKeys[2] + " TEXT DEFAULT NULL, " +
                accountKeys[3] + " TEXT DEFAULT 'RUB' NOT NULL, " +
                accountKeys[4] + " TEXT DEFAULT '0' NOT NULL, " +
                ")");

        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + "ACCOUNT_NAME_IDX ON " + accountTable + "(" +
                accountCols.get("name") +
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE " + operationsTable + " (" +
                operationsKeys[0] +" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                operationsKeys[1] +" TEXT DEFAULT NULL, " +
                operationsKeys[2] +" DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                operationsKeys[3] +" INTEGER NOT NULL, " +
                operationsKeys[4] +" INTEGER DEFAULT NULL, " +
                operationsKeys[5] +" TEXT DEFAULT '0', " +
                operationsKeys[6] +" REAL NOT NULL" +
                " FOREIGN KEY (" + operationsKeys[3] + ") REFERENCES " + accountTable + " (" + accountKeys[0] + ")" +
                " FOREIGN KEY (" + operationsKeys[4] + ") REFERENCES " + accountTable + " (" + accountKeys[0] + ")" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public void addAccount(Account account) {
        //for logging
        Log.d("addAccount", account.getName());

        // 1. get reference to writable DB
        SQLiteDatabase db = getWritableDatabase();
        assert db != null;

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(accountCols.get("name"), account.getName());
        values.put(accountCols.get("description"), account.getDescription());
        values.put(accountCols.get("currency"), account.getCurrency().toString());
        values.put(accountCols.get("amount"), account.getAmount().toString());

        // 3. insert
        db.insert(accountTable, null, values);

        // 4. close
        db.close();
    }

    public void addOperation(Operation operation) {
        //for logging
        Log.d("addOperation", operation.getAmountCharged().toPlainString());

        // 1. get reference to writable DB
        SQLiteDatabase db = getWritableDatabase();
        assert db != null;

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(operationsCols.get("description"), operation.getDescription());
        if(operation.getTime() != null)
            values.put(operationsCols.get("time"), operation.getTimeString());
        values.put(operationsCols.get("charger"), operation.getCharger().getId());
        if(operation.getReceiver() != null)
            values.put(operationsCols.get("receiver"), operation.getReceiver().getId());
        values.put(operationsCols.get("amount"), operation.getAmountCharged().toString());
        values.put(operationsCols.get("comission"), operation.getConvertingComission());

        // 3. insert
        db.insert(operationsTable, null, values);

        // 4. close
        db.close();
    }

    public Account getAccount(long id) {
        // 1. get reference to readable DB
        SQLiteDatabase db = getReadableDatabase();
        assert db != null;

        // 2. build query
        Cursor cursor = db.query(accountTable, accountKeys, " id = ?", new String[] { String.valueOf(id) }, // d. selections args
                                 null, // e. group by
                                 null, // f. having
                                 null, // g. order by
                                 null); // h. limit

        // 3. if we got results get the first one
        if (cursor != null) {
            cursor.moveToFirst();

            // 4. build book object
            Account acc = new Account();
            acc.setId(cursor.getLong(0));
            acc.setName(cursor.getString(1));
            acc.setDescription(cursor.getString(2));
            acc.setCurrency(Currency.getInstance(cursor.getString(3)));
            acc.setAmount(new BigDecimal(cursor.getString(2)));

            //log
            Log.d("getAccount(" + id + ")", acc.getName());
            return acc;
        }

        return null;
    }

    public Operation getOperaion(long id) {
        // 1. get reference to readable DB
        SQLiteDatabase db = getReadableDatabase();
        assert db != null;

        // 2. build query
        Cursor cursor = db.query(operationsTable, operationsKeys, " id = ?", new String[] { String.valueOf(id) }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        // 3. if we got results get the first one
        if (cursor != null) {
            cursor.moveToFirst();

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
        ContentValues values = new ContentValues();
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
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
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

    public void deleteAccount(Account account) {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        // 2. delete
        db.delete(accountTable, //table name
                accountCols.get("ID") + " = ?",  // selections
                new String[] { String.valueOf(account.getId()) }); //selections args

        // 3. close
        db.close();

        //log
        Log.d("deleteAccount", account.getName());
    }

    public void deleteOperation(Operation operation) {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;

        // 2. delete
        db.delete(operationsTable, //table name
                operationsCols.get("ID") + " = ?",  // selections
                new String[] { String.valueOf(operation.getId()) }); //selections args

        // 3. close
        db.close();

        //log
        Log.d("deleteOperation", operation.getAmountCharged().toPlainString());
    }
}

