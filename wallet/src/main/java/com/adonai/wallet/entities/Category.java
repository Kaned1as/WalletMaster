package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;

/**
 * Created by adonai on 23.02.14.
 */
public class Category extends Entity {
    public static final String TABLE_NAME = "categories";
    final public static int EXPENSE = 0;
    final public static int INCOME = 1;

    private String name;
    private int type;
    private Account preferredAccount;

    public Category() {
        super(DatabaseDAO.EntityType.CATEGORY);
    }

    public Category(String name, int type) {
        super(DatabaseDAO.EntityType.CATEGORY);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Account getPreferredAccount() {
        return preferredAccount;
    }

    public void setPreferredAccount(Account preferredAccount) {
        this.preferredAccount = preferredAccount;
    }

    @Override
    public long persist(DatabaseDAO dao) {
        Log.d("addCategory", getName());
        final ContentValues values = new ContentValues(3);
        if(getId() != null) // use with caution
            values.put(DatabaseDAO.CategoriesFields._id.toString(), getId());

        values.put(DatabaseDAO.CategoriesFields.NAME.toString(), getName());
        values.put(DatabaseDAO.CategoriesFields.TYPE.toString(), getType());
        if(getPreferredAccount() != null)
            values.put(DatabaseDAO.CategoriesFields.PREFERRED_ACCOUNT.toString(), getPreferredAccount().getId());

        return dao.insert(values, TABLE_NAME);
    }

    @Override
    public int update(DatabaseDAO dao) {
        final ContentValues values = new ContentValues(3);
        values.put(DatabaseDAO.CategoriesFields._id.toString(), getId());
        values.put(DatabaseDAO.CategoriesFields.NAME.toString(), getName());
        values.put(DatabaseDAO.CategoriesFields.TYPE.toString(), getType());
        if(getPreferredAccount() != null)
            values.put(DatabaseDAO.CategoriesFields.PREFERRED_ACCOUNT.toString(), getPreferredAccount().getId());

        return dao.update(values, TABLE_NAME);
    }

    @Override
    public int delete(DatabaseDAO dao) {
        return dao.delete(getId(), TABLE_NAME);
    }
}
