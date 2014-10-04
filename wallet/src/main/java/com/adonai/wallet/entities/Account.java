package com.adonai.wallet.entities;

import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.sync.SyncProtocol;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * Entity representing an account
 * <p>
 * Required fields:
 * <ol>
 *     <li>name</li>
 *     <li>currency</li>
 *     <li>amount</li>
 * </ol>
 * </p>
 * <p>
 * Optional fields:
 * <ol>
 *     <li>description</li>
 *     <li>color</li>
 * </ol>
 * </p>
 * @author Adonai
 */
@DatabaseTable(daoClass = EntityDao.class)
public class Account extends Entity {

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Currency currency;

    @DatabaseField(canBeNull = false)
    private BigDecimal amount;

    @DatabaseField
    private String description;

    @DatabaseField
    private Integer color;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public static Account fromProtoEntity(SyncProtocol.Entity entity) {
        final Account temp = new Account();
        temp.setId(UUID.fromString(entity.getID()));
        temp.setDeleted(entity.getDeleted());
        temp.setLastModified(new Date(entity.getLastModified()));

        temp.setName(entity.getAccount().getName());
        temp.setAmount(new BigDecimal(entity.getAccount().getAmount()));
        temp.setColor(entity.getAccount().getColor());
        temp.setDescription(entity.getAccount().getDescription());
        temp.setCurrency(new Currency(entity.getAccount().getCurrency()));
        return temp;
    }

    public SyncProtocol.Entity toProtoEntity() {
        SyncProtocol.Account acc = SyncProtocol.Account.newBuilder()
                .setName(getName())
                .setAmount(getAmount().toPlainString())
                .setColor(getColor())
                .setDescription(getDescription())
                .setCurrency(getCurrency().getCode())
                .build();

        return SyncProtocol.Entity.newBuilder()
                .setID(getId().toString())
                .setDeleted(isDeleted())
                .setLastModified(getLastModified().getTime())
                .setAccount(acc)
                .build();

    }
}
