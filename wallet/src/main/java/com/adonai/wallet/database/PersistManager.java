package com.adonai.wallet.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Action;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Operation;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by adonai on 29.06.14.
 */
public class PersistManager extends OrmLiteSqliteOpenHelper {

    private static final String TAG = PersistManager.class.getSimpleName();

    //имя файла базы данных который будет храниться в /data/data/APPNAME/DATABASE_NAME
    private static final String DATABASE_NAME ="wallet.db";

    //с каждым увеличением версии, при нахождении в устройстве БД с предыдущей версией будет выполнен метод onUpgrade();
    private static final int DATABASE_VERSION = 1;

    //ссылки на DAO соответсвующие сущностям, хранимым в БД
    private Dao<Account, UUID> accountDao = null;
    private Dao<Budget, UUID> budgetDao = null;
    private Dao<BudgetItem, UUID> budgetItemDao = null;
    private Dao<Category, UUID> categoryDao = null;
    private Dao<Currency, String> currencyDao = null;
    private Dao<Operation, UUID> operationDao = null;
    private Dao<Action, UUID> actionDao = null;
    private final Context mContext;

    public PersistManager(Context context){
        super(context,DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    //Выполняется, когда файл с БД не найден на устройстве
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Account.class);
            TableUtils.createTable(connectionSource, Budget.class);
            TableUtils.createTable(connectionSource, BudgetItem.class);
            TableUtils.createTable(connectionSource, Category.class);
            TableUtils.createTable(connectionSource, Currency.class);
            TableUtils.createTable(connectionSource, Operation.class);
            TableUtils.createTable(connectionSource, Action.class);
        } catch (SQLException e) {
            Log.e(TAG, "error creating DB " + DATABASE_NAME);
            throw new RuntimeException(e);
        }
    }

    //Выполняется, когда БД имеет версию отличную от текущей
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVer, int newVer) {

    }

    public Dao<Account, UUID> getAccountDao() throws SQLException {
        if(accountDao == null)
            accountDao = getDao(Account.class);
        return accountDao;
    }

    public Dao<Budget, UUID> getBudgetDao() throws SQLException {
        if(budgetDao == null)
            budgetDao = getDao(Budget.class);
        return budgetDao;
    }

    public Dao<BudgetItem, UUID> getBudgetItemDao() throws SQLException {
        if(budgetItemDao == null)
            budgetItemDao = getDao(BudgetItem.class);
        return budgetItemDao;
    }

    public Dao<Category, UUID> getCategoryDao() throws SQLException {
        if(categoryDao == null)
            categoryDao = getDao(Category.class);
        return categoryDao;
    }

    public Dao<Currency, String> getCurrencyDao() throws SQLException {
        if(currencyDao == null)
            currencyDao = getDao(Currency.class);
        return currencyDao;
    }

    public Dao<Operation, UUID> getOperationDao() throws SQLException {
        if(operationDao == null)
            operationDao = getDao(Operation.class);
        return operationDao;
    }

    public Dao<Action, UUID> getActionDao() throws SQLException {
        if(actionDao == null)
            actionDao = getDao(Action.class);
        return actionDao;
    }

    //выполняется при закрытии приложения
    @Override
    public void close() {
        super.close();
    }
}
