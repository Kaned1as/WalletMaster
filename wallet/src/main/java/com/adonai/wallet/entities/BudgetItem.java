package com.adonai.wallet.entities;

import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;

import java.math.BigDecimal;

import static com.adonai.wallet.DatabaseDAO.BudgetItemFields;

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
            bi.setId(cursor.getString(BudgetItemFields._id.ordinal()));
            bi.setCategory(Category.getFromDB(dao, cursor.getString(BudgetItemFields.CATEGORY.ordinal())));
            bi.setParentBudget(Budget.getFromDB(dao, cursor.getString(BudgetItemFields.PARENT_BUDGET.ordinal())));
            bi.setMaxAmount(new BigDecimal(cursor.getString(BudgetItemFields.MAX_AMOUNT.ordinal())));
            Log.d("Entity Serialization", "getBudgetItem(" + id + "), category name: " + bi.getCategory().getName());
            cursor.close();
            return bi;
        }

        cursor.close();
        return null;
    }
}
