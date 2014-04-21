package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.sync.SyncProtocol;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author adonai
 */
public class Account extends Entity {
    public static final String TABLE_NAME = "accounts";
    private String name;
    private String description;
    private Currency currency;
    private BigDecimal amount;
    private Integer color;

    public Account() {
        super(DatabaseDAO.EntityType.ACCOUNT);
    }

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
    public long persist(DatabaseDAO dao) {
        Log.d("addAccount", getName());

        final ContentValues values = new ContentValues(5);
        if(getId() != null) // use with caution
            values.put(DatabaseDAO.AccountFields._id.toString(), getId());

        values.put(DatabaseDAO.AccountFields.NAME.toString(), getName());
        values.put(DatabaseDAO.AccountFields.DESCRIPTION.toString(), getDescription());
        values.put(DatabaseDAO.AccountFields.CURRENCY.toString(), getCurrency().toString());
        values.put(DatabaseDAO.AccountFields.AMOUNT.toString(), getAmount().toPlainString());
        values.put(DatabaseDAO.AccountFields.COLOR.toString(), getColor());

        return dao.insert(values, TABLE_NAME);
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

        return dao.update(values, TABLE_NAME);
    }

    @Override
    public int delete(DatabaseDAO dao) {
        return dao.delete(getId(), TABLE_NAME);
    }

    /**
     * Select all entities that are known to client
     * Note that entities added on device are not in this list
     * @param dao
     * @return
     */
    public static List<Long> getKnownIDs(DatabaseDAO dao) {
        final List<Long> result = new ArrayList<>();
        final Cursor selections = dao.select(
                "SELECT " + DatabaseDAO.AccountFields._id +
                " FROM " + TABLE_NAME +
                " WHERE " + DatabaseDAO.AccountFields._id + " NOT IN (" +
                        "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
                        " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
                        " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + DatabaseDAO.EntityType.ACCOUNT.ordinal() +
                        " AND " + DatabaseDAO.ActionsFields.ACTION_TYPE + " = " + DatabaseDAO.ActionType.ADD.ordinal() +
                        ")" +
                " UNION ALL " +
                "SELECT " + DatabaseDAO.ActionsFields.DATA_ID +
                " FROM " + DatabaseDAO.ACTIONS_TABLE_NAME +
                " WHERE " + DatabaseDAO.ActionsFields.DATA_TYPE + " = " + DatabaseDAO.EntityType.ACCOUNT.ordinal() +
                " AND " + DatabaseDAO.ActionsFields.ACTION_TYPE + " = " + DatabaseDAO.ActionType.DELETE.ordinal()
                , null);
        while (selections.moveToNext())
            result.add(selections.getLong(0));
        selections.close();
        return result;
    }
}
