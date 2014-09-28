package com.adonai.wallet.entities;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Created by adonai on 19.06.14.
 */
@DatabaseTable(tableName = "budget_item", daoClass = EntityDao.class)
public class BudgetItem extends Entity {

    public BudgetItem() {
    }

    public BudgetItem(Budget parentBudget) {
        this.parentBudget = parentBudget;
    }

    @DatabaseField(canBeNull = false, foreign = true)
    private Budget parentBudget;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Category category;

    @DatabaseField
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

    public BigDecimal getProgress() {
        final BigDecimal result = BigDecimal.ZERO;
        if(parentBudget == null) // parent budget is not set, nothing to count
            return result;

        return getAmountForBudget(parentBudget, category);
    }

    public BigDecimal getAmountForBudget(Budget budget, Category category) {
        final BigDecimal sum;
        GenericRawResults<String[]> results;
        if(budget.getCoveredAccount() == null) { // all accounts
            results = DbProvider.getHelper().getOperationDao().queryRaw("SELECT SUM(amount) FROM operations WHERE category = ? AND time BETWEEN ? AND ?", category.getId().toString(), String.valueOf(budget.getStartTime().getTime()), String.valueOf(budget.getEndTime().getTime()));
        }else {
            results = DbProvider.getHelper().getOperationDao().queryRaw("SELECT SUM(amount) FROM operations WHERE orderer = ? AND category = ? AND time BETWEEN ? AND ?", budget.getCoveredAccount().getId().toString(), category.getId().toString(), String.valueOf(budget.getStartTime().getTime()), String.valueOf(budget.getEndTime().getTime()));
        }

        try {
            String[] res = results.getFirstResult();
            sum = new BigDecimal(res[0]);
            results.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sum;
    }
}
