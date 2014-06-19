package com.adonai.wallet.entities;

import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;

import java.math.BigDecimal;

/**
 * Created by adonai on 19.06.14.
 */
@EntityDescriptor(type = DatabaseDAO.EntityType.BUDGET_ITEMS)
public class BudgetItem extends Entity {

    private Budget parentBudget;
    private Category category;
    private BigDecimal maxAmount;

    public Budget getParentBudget() {
        return parentBudget;
    }

    public void setParentBudget(Budget parentBudget) {
        this.parentBudget = parentBudget;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public String persist(DatabaseDAO dao) {
        return null;
    }

    @Override
    public int update(DatabaseDAO dao) {
        return 0;
    }

    public static BudgetItem getFromDB(DatabaseDAO dao, String id) {
        final Cursor cursor = dao.get(DatabaseDAO.EntityType.BUDGET_ITEMS, id);
        if (cursor.moveToFirst()) {
            final BudgetItem bi = new BudgetItem();
            bi.setId(cursor.getString(DatabaseDAO.AccountFields._id.ordinal()));
            bi.setName(cursor.getString(DatabaseDAO.AccountFields.NAME.ordinal()));
            bi.setDescription(cursor.getString(DatabaseDAO.AccountFields.DESCRIPTION.ordinal()));
            bi.setCurrency(dao.getCurrency(cursor.getString(DatabaseDAO.AccountFields.CURRENCY.ordinal())));
            bi.setAmount(new BigDecimal(cursor.getString(DatabaseDAO.AccountFields.AMOUNT.ordinal())));
            bi.setColor(cursor.getInt(DatabaseDAO.AccountFields.COLOR.ordinal()));

            Log.d("getAccount(" + id + ")", bi.getName());
            cursor.close();
            return bi;
        }

        cursor.close();
        return null;
    }
}
