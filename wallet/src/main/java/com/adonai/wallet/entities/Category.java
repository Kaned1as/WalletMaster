package com.adonai.wallet.entities;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.sync.SyncProtocol;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
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
@DatabaseTable(daoClass = EntityDao.class)
public class Category extends Entity {

    public enum CategoryType {
        EXPENSE,
        INCOME,
        TRANSFER
    }

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private CategoryType type;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
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

    public static Category fromProtoCategory(SyncProtocol.Category category) throws SQLException {
        final Category tempCategory = new Category();
        tempCategory.setId(UUID.fromString(category.getID()));
        tempCategory.setName(category.getName());
        tempCategory.setType(CategoryType.values()[category.getType()]);
        if(category.hasPreferredAccount())
            tempCategory.setPreferredAccount(DbProvider.getHelper().getAccountDao().queryForId(UUID.fromString(category.getPreferredAccount())));

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
