package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.sync.SyncProtocol;

import java.util.UUID;

@EntityDescriptor(type = DatabaseDAO.EntityType.CATEGORIES)
public class Category extends Entity {
    final public static int EXPENSE = 0;
    final public static int INCOME = 1;
    final public static int TRANSFER = 2;

    private String name;
    private int type;
    private Account preferredAccount;

    public Category() {
    }

    public Category(String name, int type) {
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
    public String persist(DatabaseDAO dao) {
        Log.d("addCategory", getName());

        final ContentValues values = new ContentValues(3);
        if(getId() != null) // use with caution
            values.put(DatabaseDAO.CategoriesFields._id.toString(), getId());
        else
            values.put(DatabaseDAO.CategoriesFields._id.toString(), UUID.randomUUID().toString());

        values.put(DatabaseDAO.CategoriesFields.NAME.toString(), getName());
        values.put(DatabaseDAO.CategoriesFields.TYPE.toString(), getType());
        if(getPreferredAccount() != null)
            values.put(DatabaseDAO.CategoriesFields.PREFERRED_ACCOUNT.toString(), getPreferredAccount().getId());

        long row = dao.insert(values, entityType.toString());
        if(row > 0)
            return values.getAsString(DatabaseDAO.CategoriesFields._id.toString());
        else
            return null;
    }

    @Override
    public int update(DatabaseDAO dao) {
        final ContentValues values = new ContentValues(3);
        values.put(DatabaseDAO.CategoriesFields._id.toString(), getId());
        values.put(DatabaseDAO.CategoriesFields.NAME.toString(), getName());
        values.put(DatabaseDAO.CategoriesFields.TYPE.toString(), getType());
        if(getPreferredAccount() != null)
            values.put(DatabaseDAO.CategoriesFields.PREFERRED_ACCOUNT.toString(), getPreferredAccount().getId());

        return dao.update(values, entityType.toString());
    }

    public static Category getFromDB(DatabaseDAO dao, String id) {
        final Cursor cursor = dao.get(DatabaseDAO.EntityType.CATEGORIES, id);
        if (cursor.moveToFirst()) {
            final Category cat = new Category();
            cat.setId(cursor.getString(DatabaseDAO.CategoriesFields._id.ordinal()));
            cat.setName(cursor.getString(DatabaseDAO.CategoriesFields.NAME.ordinal()));
            cat.setType(cursor.getInt(DatabaseDAO.CategoriesFields.TYPE.ordinal()));
            if(!cursor.isNull(DatabaseDAO.CategoriesFields.PREFERRED_ACCOUNT.ordinal()))
                cat.setPreferredAccount(Account.getFromDB(dao, cursor.getString(DatabaseDAO.CategoriesFields.PREFERRED_ACCOUNT.ordinal())));
            cursor.close();

            Log.d(String.format("getCategory(%s)", id), cat.getName());
            return cat;
        }

        cursor.close();
        return null;
    }

    public static Category fromProtoCategory(SyncProtocol.Category category, DatabaseDAO dao) {
        final Category tempCategory = new Category();
        tempCategory.setId(category.getID());
        tempCategory.setName(category.getName());
        tempCategory.setType(category.getType());
        if(category.hasPreferredAccount())
            tempCategory.setPreferredAccount(Account.getFromDB(dao, category.getPreferredAccount()));

        return tempCategory;
    }

    public static SyncProtocol.Category toProtoCategory(Category category) {
        final SyncProtocol.Category.Builder builder = SyncProtocol.Category.newBuilder()
                .setID(category.getId())
                .setName(category.getName())
                .setType(category.getType());

        if(category.getPreferredAccount() != null)
            builder.setPreferredAccount(category.getPreferredAccount().getId());

        return builder.build();
    }
}
