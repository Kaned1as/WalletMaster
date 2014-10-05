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

    @DatabaseField(columnName = "preferred_account", foreign = true, foreignAutoRefresh = true)
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

    public static Category fromProtoEntity(SyncProtocol.Entity entity) throws SQLException {
        final Category tempCategory = new Category();
        tempCategory.setId(UUID.fromString(entity.getID()));
        tempCategory.setDeleted(entity.getDeleted());

        tempCategory.setName(entity.getCategory().getName());
        tempCategory.setType(CategoryType.values()[entity.getCategory().getType()]);
        if(entity.getCategory().hasPreferredAccount())
            tempCategory.setPreferredAccount(DbProvider.getHelper().getAccountDao().queryForId(UUID.fromString(entity.getCategory().getPreferredAccount())));

        return tempCategory;
    }

    public SyncProtocol.Entity toProtoEntity() {
        final SyncProtocol.Category.Builder builder = SyncProtocol.Category.newBuilder()
                .setName(getName())
                .setType(getType().ordinal());
        if(getPreferredAccount() != null)
            builder.setPreferredAccount(getPreferredAccount().getId().toString());
        SyncProtocol.Category cat = builder.build();

        return SyncProtocol.Entity.newBuilder()
                .setID(getId().toString())
                .setDeleted(isDeleted())
                //.setLastModified(getLastModified().getTime())  // don't send server time to itself
                .setCategory(cat)
                .build();
    }
}
