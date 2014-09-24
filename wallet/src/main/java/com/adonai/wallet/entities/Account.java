package com.adonai.wallet.entities;

import com.adonai.wallet.sync.SyncProtocol;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
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
@DatabaseTable
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

    public static Account fromProtoAccount(SyncProtocol.Account account) {
        final Account temp = new Account();
        temp.setId(UUID.fromString(account.getID()));
        temp.setName(account.getName());
        temp.setAmount(new BigDecimal(account.getAmount()));
        temp.setColor(account.getColor());
        temp.setDescription(account.getDescription());
        temp.setCurrency(new Currency(account.getCurrency()));
        return temp;
    }

    public static SyncProtocol.Account toProtoAccount(Account account) {
        return SyncProtocol.Account.newBuilder()
                .setID(account.getId().toString())
                .setName(account.getName())
                .setAmount(account.getAmount().toPlainString())
                .setColor(account.getColor())
                .setDescription(account.getDescription())
                .setCurrency(account.getCurrency().getCode())
                .build();
    }
}
