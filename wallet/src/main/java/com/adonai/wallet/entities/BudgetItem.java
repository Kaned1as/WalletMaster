package com.adonai.wallet.entities;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

/**
 * Entity representing a budget item. Budget item tracks sum of all expenses for specified period
 * of time for specified category
 *
 * @see com.adonai.wallet.entities.Budget
 * @author Adonai
 */
@DatabaseTable(tableName = "budget_item", daoClass = EntityDao.class)
public class BudgetItem extends Entity {

    public BudgetItem() {
    }

    public BudgetItem(Budget parentBudget) {
        this.parentBudget = parentBudget;
    }

    @DatabaseField(columnName = "parent_budget", canBeNull = false, foreign = true)
    private Budget parentBudget;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Category category;

    @DatabaseField(columnName = "max_amount")
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

    /**
     * Get progress for the current day
     * @return BigDecimal representing spent amount today
     */
    public BigDecimal getDailyProgress() {
        final BigDecimal sum;
        GenericRawResults<String[]> results;

        Calendar currentDay = Calendar.getInstance();
        currentDay.set(Calendar.HOUR_OF_DAY, 0);
        currentDay.set(Calendar.MINUTE, 0);
        currentDay.set(Calendar.SECOND, 0);
        currentDay.set(Calendar.MILLISECOND, 0);
        
        if(parentBudget.getCoveredAccount() == null) { // all accounts
            results = DbProvider.getHelper().getOperationDao().queryRaw("SELECT SUM(amount) FROM operation WHERE category_id = ? AND time > ?", category.getId().toString(), String.valueOf(currentDay.getTime().getTime()));
        } else {
            results = DbProvider.getHelper().getOperationDao().queryRaw("SELECT SUM(amount) FROM operation WHERE orderer_id = ? AND category_id = ? AND time > ?", parentBudget.getCoveredAccount().getId().toString(), category.getId().toString(), String.valueOf(currentDay.getTime().getTime()));
        }

        try {
            String[] res = results.getFirstResult();
            sum = res[0] == null ? BigDecimal.ZERO : new BigDecimal(res[0]);
            results.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sum;
    }

    public BigDecimal getAmountForBudget(Budget budget, Category category) {
        final BigDecimal sum;
        GenericRawResults<String[]> results;
        if(budget.getCoveredAccount() == null) { // all accounts
            results = DbProvider.getHelper().getOperationDao().queryRaw("SELECT SUM(amount) FROM operation WHERE category_id = ? AND time BETWEEN ? AND ?", category.getId().toString(), String.valueOf(budget.getStartTime().getTime()), String.valueOf(budget.getEndTime().getTime()));
        } else {
            results = DbProvider.getHelper().getOperationDao().queryRaw("SELECT SUM(amount) FROM operation WHERE orderer_id = ? AND category_id = ? AND time BETWEEN ? AND ?", budget.getCoveredAccount().getId().toString(), category.getId().toString(), String.valueOf(budget.getStartTime().getTime()), String.valueOf(budget.getEndTime().getTime()));
        }

        try {
            String[] res = results.getFirstResult();
            sum = res[0] == null ? BigDecimal.ZERO : new BigDecimal(res[0]);
            results.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sum;
    }
}
