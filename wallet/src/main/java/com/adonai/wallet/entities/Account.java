package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.sync.SyncProtocol;

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
@EntityDescriptor(type = DatabaseDAO.EntityType.ACCOUNTS)
public class Account extends Entity {
    private String name;
    private String description;
    private Currency currency;
    private BigDecimal amount;
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
        temp.setId(account.getID());
        temp.setName(account.getName());
        temp.setAmount(new BigDecimal(account.getAmount()));
        temp.setColor(account.getColor());
        temp.setDescription(account.getDescription());
        temp.setCurrency(new Currency(account.getCurrency()));
        return temp;
    }

    public static SyncProtocol.Account toProtoAccount(Account account) {
        return SyncProtocol.Account.newBuilder()
                .setID(account.getId())
                .setName(account.getName())
                .setAmount(account.getAmount().toPlainString())
                .setColor(account.getColor())
                .setDescription(account.getDescription())
                .setCurrency(account.getCurrency().getCode())
                .build();
    }

    @Override
    public String persist(DatabaseDAO dao) {
        Log.d("addAccount", getName());

        final ContentValues values = new ContentValues(5);
        if(getId() != null) // use with caution
            values.put(DatabaseDAO.AccountFields._id.toString(), getId());
        else
            values.put(DatabaseDAO.AccountFields._id.toString(), UUID.randomUUID().toString());

        values.put(DatabaseDAO.AccountFields.NAME.toString(), getName());
        values.put(DatabaseDAO.AccountFields.DESCRIPTION.toString(), getDescription());
        values.put(DatabaseDAO.AccountFields.CURRENCY.toString(), getCurrency().toString());
        values.put(DatabaseDAO.AccountFields.AMOUNT.toString(), getAmount().toPlainString());
        values.put(DatabaseDAO.AccountFields.COLOR.toString(), getColor());

        long row = dao.insert(values, entityType.toString());
        if(row > 0)
            return values.getAsString(DatabaseDAO.AccountFields._id.toString());
        else
            return null;
    }

    @Override
    public int update(DatabaseDAO dao) {
        final ContentValues values = new ContentValues();
        values.put(DatabaseDAO.AccountFields._id.toString(), getId());
        values.put(DatabaseDAO.AccountFields.NAME.toString(), getName());
        values.put(DatabaseDAO.AccountFields.DESCRIPTION.toString(), getDescription());
        values.put(DatabaseDAO.AccountFields.CURRENCY.toString(), getCurrency().toString());
        values.put(DatabaseDAO.AccountFields.AMOUNT.toString(), getAmount().toPlainString());
        values.put(DatabaseDAO.AccountFields.COLOR.toString(), getColor());

        return dao.update(values, entityType.toString());
    }

    public static Account getFromDB(DatabaseDAO dao, String id) {
        final Cursor cursor = dao.get(DatabaseDAO.EntityType.ACCOUNTS, id);
        if (cursor.moveToFirst()) {
            final Account acc = new Account();
            acc.setId(cursor.getString(DatabaseDAO.AccountFields._id.ordinal()));
            acc.setName(cursor.getString(DatabaseDAO.AccountFields.NAME.ordinal()));
            acc.setDescription(cursor.getString(DatabaseDAO.AccountFields.DESCRIPTION.ordinal()));
            acc.setCurrency(dao.getCurrency(cursor.getString(DatabaseDAO.AccountFields.CURRENCY.ordinal())));
            acc.setAmount(new BigDecimal(cursor.getString(DatabaseDAO.AccountFields.AMOUNT.ordinal())));
            acc.setColor(cursor.getInt(DatabaseDAO.AccountFields.COLOR.ordinal()));

            Log.d("getAccount(" + id + ")", acc.getName());
            cursor.close();
            return acc;
        }

        cursor.close();
        return null;
    }
}
