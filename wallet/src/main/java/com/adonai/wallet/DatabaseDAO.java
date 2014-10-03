package com.adonai.wallet;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Entity;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;

/**
 * Database helper instance
 *
 * Database entity tables always contains current working copy
 * Actions contain original data
 */
public class DatabaseDAO /*extends SQLiteOpenHelper*/ {
//    @Override
//    public void onCreate(SQLiteDatabase sqLiteDatabase) {
//        mDatabase = sqLiteDatabase;
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.ACCOUNTS + " (" +
//                AccountFields._id + " TEXT PRIMARY KEY, " +
//                AccountFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
//                AccountFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
//                AccountFields.CURRENCY + " TEXT DEFAULT 'RUB' NOT NULL, " +
//                AccountFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
//                AccountFields.COLOR + " TEXT DEFAULT NULL" +
//                ")");
//        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX ACCOUNT_NAME_IDX ON " + EntityType.ACCOUNTS + " (" + AccountFields.NAME + ")");
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.OPERATIONS + " (" +
//                OperationsFields._id + " TEXT PRIMARY KEY, " +
//                OperationsFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
//                OperationsFields.CATEGORY + " TEXT NOT NULL, " +
//                OperationsFields.TIME + " TIMESTAMP DEFAULT (STRFTIME('%s', 'now') * 1000) NOT NULL, " +
//                OperationsFields.CHARGER + " TEXT DEFAULT NULL, " +
//                OperationsFields.RECEIVER + " TEXT DEFAULT NULL, " +
//                OperationsFields.AMOUNT + " TEXT DEFAULT '0' NOT NULL, " +
//                OperationsFields.CONVERT_RATE + " REAL DEFAULT NULL, " +
//                " FOREIGN KEY (" + OperationsFields.CATEGORY + ") REFERENCES " + EntityType.CATEGORIES + " (" + CategoriesFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE," +
//                " FOREIGN KEY (" + OperationsFields.CHARGER + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE," + // delete associated transactions
//                " FOREIGN KEY (" + OperationsFields.RECEIVER + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" + // delete associated transactions
//                ")");
//        sqLiteDatabase.execSQL("CREATE INDEX OP_TIME_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.TIME + ")");
//        sqLiteDatabase.execSQL("CREATE INDEX OP_CATEGORY_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.CATEGORY + ")");
//        sqLiteDatabase.execSQL("CREATE INDEX OP_CHARGER_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.CHARGER + ")");
//        sqLiteDatabase.execSQL("CREATE INDEX OP_RECEIVER_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.RECEIVER + ")");
//        sqLiteDatabase.execSQL("CREATE INDEX OP_AMOUNT_IDX ON " + EntityType.OPERATIONS + " (" +  OperationsFields.AMOUNT + ")");
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + Currency.TABLE_NAME + " (" +
//                CurrenciesFields.CODE + " TEXT PRIMARY KEY, " +
//                CurrenciesFields.DESCRIPTION + " TEXT DEFAULT NULL, " +
//                CurrenciesFields.USED_IN + " TEXT DEFAULT NULL" +
//                ")");
//        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX CURRENCY_IDX ON " + Currency.TABLE_NAME + " (" +  CurrenciesFields.CODE + ")");
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.CATEGORIES + " (" +
//                CategoriesFields._id + " TEXT PRIMARY KEY, " +
//                CategoriesFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
//                CategoriesFields.TYPE + " INTEGER DEFAULT 0 NOT NULL, " +
//                CategoriesFields.PREFERRED_ACCOUNT + " TEXT DEFAULT NULL, " +
//                " FOREIGN KEY (" + CategoriesFields.PREFERRED_ACCOUNT + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE SET NULL ON UPDATE CASCADE" +
//                ")");
//        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX CATEGORY_UNIQUE_NAME_IDX ON " + EntityType.CATEGORIES + " (" +  CategoriesFields.NAME + "," + CategoriesFields.TYPE + ")");
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.BUDGETS + " (" +
//                BudgetFields._id + " TEXT PRIMARY KEY, " +
//                BudgetFields.NAME + " TEXT DEFAULT '' NOT NULL, " +
//                BudgetFields.START_TIME + " TIMESTAMP NOT NULL, " +
//                BudgetFields.END_TIME + " TIMESTAMP NOT NULL, " +
//                BudgetFields.COVERED_ACCOUNT + " TEXT DEFAULT NULL, " +
//                " FOREIGN KEY (" + BudgetFields.COVERED_ACCOUNT + ") REFERENCES " + EntityType.ACCOUNTS + " (" + AccountFields._id + ") ON DELETE SET NULL ON UPDATE CASCADE" +
//                ")");
//        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX BUDGET_UNIQUE_NAME_IDX ON " + EntityType.BUDGETS + " (" +  BudgetFields.NAME + ")");
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + EntityType.BUDGET_ITEMS + " (" +
//                BudgetItemFields._id + " TEXT PRIMARY KEY, " +
//                BudgetItemFields.PARENT_BUDGET + " TEXT NOT NULL, " +
//                BudgetItemFields.CATEGORY + " TEXT NOT NULL, " +
//                BudgetItemFields.MAX_AMOUNT + " TEXT NOT NULL, " +
//                " FOREIGN KEY (" + BudgetItemFields.CATEGORY + ") REFERENCES " + EntityType.CATEGORIES + " (" + CategoriesFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" +
//                " FOREIGN KEY (" + BudgetItemFields.PARENT_BUDGET + ") REFERENCES " + EntityType.BUDGETS + " (" + BudgetFields._id + ") ON DELETE CASCADE ON UPDATE CASCADE" +
//                ")");
//        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX BUDGET_ITEM_UNIQUE_CATEGORY_PARENT_IDX ON " + EntityType.BUDGET_ITEMS + " (" +  BudgetItemFields.PARENT_BUDGET + "," + BudgetItemFields.CATEGORY + ")");
//
//
//        sqLiteDatabase.execSQL("CREATE TABLE " + ACTIONS_TABLE_NAME + " (" +
//                ActionsFields.DATA_ID + " TEXT NOT NULL, " +
//                ActionsFields.DATA_TYPE + " INTEGER NOT NULL, " +
//                ActionsFields.ORIGINAL_DATA + " TEXT, " +
//                " PRIMARY KEY(" + ActionsFields.DATA_ID + ", " + ActionsFields.DATA_TYPE + ")" +
//                ")");
//
//        // test accounts
//        /*
//        sqLiteDatabase.beginTransaction(); // initial fill
//        final Random rand = new Random();
//        for(int i = 0; i < 100; ++i) {
//            final ContentValues accValues = new ContentValues(5);
//            accValues.put(AccountFields._id.toString(), UUID.randomUUID().toString());
//            accValues.put(AccountFields.NAME.toString(), "Account" + i);
//            accValues.put(AccountFields.DESCRIPTION.toString(), "");
//            accValues.put(AccountFields.CURRENCY.toString(), "RUB");
//            accValues.put(AccountFields.AMOUNT.toString(), rand.nextInt(1000));
//            accValues.put(AccountFields.COLOR.toString(), Color.rgb(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));
//
//            for(int j = 0; j < 100; ++j) {
//                final ContentValues opValues = new ContentValues(7);
//                opValues.put(OperationsFields._id.toString(), UUID.randomUUID().toString());
//                opValues.put(OperationsFields.DESCRIPTION.toString(), "operation" + j); // mandatory
//                opValues.put(OperationsFields.CATEGORY.toString(), outAdd.getId()); // mandatory
//                opValues.put(OperationsFields.AMOUNT.toString(), rand.nextInt(500)); // mandatory
//                opValues.put(OperationsFields.CHARGER.toString(), accValues.getAsString(AccountFields._id.toString()));
//
//                sqLiteDatabase.insert(EntityType.OPERATIONS.toString(), null, opValues);
//            }
//
//            sqLiteDatabase.insert(EntityType.ACCOUNTS.toString(), null, accValues);
//        }
//        sqLiteDatabase.setTransactionSuccessful(); // batch insert
//        sqLiteDatabase.endTransaction();
//        */
//    }
//
//    /**
//     * Helper method to retrieve dependent entities names
//     * Useful for spinner adapters
//     *
//     * @param tableName table to query foreign keys from
//     * @param fkColumn  foreign key column in source table
//     * @param targetTableName table name containing entities
//     * @param nameColumn column representing name (for GUI needs) in target table
//     * @return joined cursor with corresponding columns sourcetable.id <---> targettable.name
//     */
//    public Cursor getForeignNameCursor(EntityType tableName, String fkColumn, EntityType targetTableName, String nameColumn) {
//        final SQLiteQueryBuilder filterBuilder = new SQLiteQueryBuilder();
//        filterBuilder.setDistinct(true);
//        filterBuilder.setTables(tableName + " AS t1" +
//                " LEFT JOIN " + targetTableName + " AS t2 ON t1." + fkColumn + " = t2._id");
//        return filterBuilder.query(mDatabase, new String[]{"t1." + fkColumn, "t2." + nameColumn}, null, null, null, null, null);
//    }
//
//
//    public Cursor getOperationsCursor(String filter) {
//        Log.d("Database filter query", "getOperationsCursor with filter");
//        final SQLiteQueryBuilder filterBuilder = new SQLiteQueryBuilder();
//
//        final StringBuilder sb = new StringBuilder(20);
//        sb.append("LOWER(");
//        sb.append("COALESCE(op.").append(OperationsFields.DESCRIPTION).append(", '')");
//        sb.append(" || ' ' || op.").append(OperationsFields.AMOUNT);
//        sb.append(" || ' ' || strftime('%d.%m.%Y', op.").append(OperationsFields.TIME).append("/1000, 'unixepoch', 'localtime')");
//        sb.append(" || ' ' || COALESCE(cats.").append(CategoriesFields.NAME).append(", '')");
//        sb.append(" || ' ' || COALESCE(charger.").append(AccountFields.NAME).append(", '')");
//        sb.append(" || ' ' || COALESCE(benefic.").append(AccountFields.NAME).append(", '')");
//
//        sb.append(") LIKE LOWER(?)");
//        filterBuilder.appendWhere(sb.toString());
//        filterBuilder.setTables(EntityType.OPERATIONS + " AS op" +
//                " LEFT JOIN " + EntityType.CATEGORIES + " AS cats " + "ON op." + OperationsFields.CATEGORY + " = " + "cats." + CategoriesFields._id +
//                " LEFT JOIN " + EntityType.ACCOUNTS + " AS charger " + "ON op." + OperationsFields.CHARGER + " = " + "charger." + AccountFields._id +
//                " LEFT JOIN " + EntityType.ACCOUNTS + " AS benefic " + "ON op." + OperationsFields.RECEIVER + " = " + "benefic." + AccountFields._id);
//        return filterBuilder.query(mDatabase, new String[]{"op.*"}, null, new String[] {"%" + filter + "%"}, null, null, null);
//    }
//

    public static <T extends Entity> long getLastServerTimestamp(Class<T> clazz) throws SQLException {
        String tableName = clazz.getAnnotation(DatabaseTable.class).tableName();
        return DbProvider.getHelper().getEntityDao(clazz).queryRawValue("select ifnull(timestamp, 0) from" + tableName + "order by timestamp desc limit 1");
    }

//    /**
//     * Select all entities that are added locally and not yet synchronized with remote database
//     * @return list of entities of specified class that were added locally
//     */
//    @SuppressWarnings("unchecked")
//    public <T extends Entity> List<T> getAdded(Class<T> clazz) {
//        final List<T> result = new ArrayList<>();
//        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
//        final Cursor selections = select(
//                "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
//                        " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
//                        " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + type.ordinal() +
//                        " AND " + DatabaseDAO.ActionsFields.ORIGINAL_DATA + " IS NULL" //  added entities
//                , null
//        );
//        while (selections.moveToNext())
//            try {
//                final Method filler = clazz.getDeclaredMethod("getFromDB", String.class);
//                final T newEntity = (T) filler.invoke(null, selections.getString(0));
//                if(newEntity != null)
//                    result.add(newEntity);
//            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
//                throw new IllegalArgumentException("No public static getFromDB method in child entity class!");
//            }
//
//        selections.close();
//        return result;
//    }
//
//    @SuppressWarnings("unchecked")
//    public <T extends Entity> List<T> getModified(Class<T> clazz) {
//        final List<T> result = new ArrayList<>();
//        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
//        final Cursor selections = select(
//                "SELECT " + DatabaseDAO.AccountFields._id +
//                " FROM " + type +
//                " WHERE " + DatabaseDAO.AccountFields._id + " IN (" +
//                    "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
//                    " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
//                    " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + type.ordinal() +
//                    " AND " + DatabaseDAO.ActionsFields.ORIGINAL_DATA + " IS NOT NULL" + // exists in original and modified version - it's modified!
//                    ")"
//                , null);
//        while (selections.moveToNext())
//            try {
//                final Method filler = clazz.getDeclaredMethod("getFromDB", String.class);
//                final T newEntity = (T) filler.invoke(null, selections.getString(0));
//                if(newEntity != null)
//                    result.add(newEntity);
//            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
//                throw new IllegalArgumentException("No public static getFromDB method in child entity class!");
//            }
//        selections.close();
//        return result;
//    }
//
//    public <T extends Entity> List<String> getDeleted(Class<T> clazz) {
//        final List<String> result = new ArrayList<>();
//        final EntityType type = clazz.getAnnotation(EntityDescriptor.class).type();
//        final Cursor selections = select(
//                "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
//                        " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
//                        " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + type.ordinal() +
//                        " AND " + DatabaseDAO.ActionsFields.DATA_ID + " NOT IN (" +
//                        "SELECT " + DatabaseDAO.AccountFields._id +
//                        " FROM " + type + // these are deleted entities - present in backup table but not exist in real entities table
//                        ")"
//                , null
//        );
//        while (selections.moveToNext())
//            result.add(selections.getString(0));
//        selections.close();
//        return result;
//    }
//
}

