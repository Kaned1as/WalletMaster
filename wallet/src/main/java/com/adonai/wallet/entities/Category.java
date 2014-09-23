package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.sync.SyncProtocol;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

/**
 * Entity representing a category that is used for operations splitting, budget and so on.
 * <p>
 * Required fields:
 * <ol>
 *     <li>name</li>
 *     <li>type</li>
 * </ol>
 * </p>
 * <p>
 * Optional fields:
 * <ol>
 *     <li>preferred account</li>
 * </ol>
 * </p>
 *
 * @author Adonai
 */
@DatabaseTable
public class Category {

    public enum CategoryType {
        EXPENSE,
        INCOME,
        TRANSFER
    }

    @DatabaseField(id = true)
    private UUID id = UUID.randomUUID();

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private CategoryType type;

    @DatabaseField(foreign = true)
    private Account preferredAccount;

    public Category() {
    }

    public Category(String name, CategoryType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public Account getPreferredAccount() {
        return preferredAccount;
    }

    public void setPreferredAccount(Account preferredAccount) {
        this.preferredAccount = preferredAccount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public static Category fromProtoCategory(SyncProtocol.Category category) {
        final Category tempCategory = new Category();
        tempCategory.setId(UUID.fromString(category.getID()));
        tempCategory.setName(category.getName());
        tempCategory.setType(CategoryType.values()[category.getType()]);
        if(category.hasPreferredAccount())
            tempCategory.setPreferredAccount(Account.getFromDB(category.getPreferredAccount()));

        return tempCategory;
    }

    public static SyncProtocol.Category toProtoCategory(Category category) {
        final SyncProtocol.Category.Builder builder = SyncProtocol.Category.newBuilder()
                .setID(category.getId().toString())
                .setName(category.getName())
                .setType(category.getType().ordinal());

        if(category.getPreferredAccount() != null)
            builder.setPreferredAccount(category.getPreferredAccount().getId().toString());

        return builder.build();
    }
}
