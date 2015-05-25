package com.adonai.wallet.entities;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
        EntityDao<Operation> dao = DbProvider.getHelper().getEntityDao(Operation.class);
        StringBuilder sb = new StringBuilder("SELECT SUM(amount) FROM operation WHERE category_id = ?");
        List<String> args = new ArrayList<>();
        args.add(category.getId().toString());

        if(parentBudget.getCoveredAccount() != null) {
            sb.append(" AND orderer_id = ?");
            args.add(parentBudget.getCoveredAccount().getId().toString());
        }
        
        // counting the beginning of current day
        Calendar currentDay = Calendar.getInstance();
        currentDay.set(Calendar.HOUR_OF_DAY, 0);
        currentDay.set(Calendar.MINUTE, 0);
        currentDay.set(Calendar.SECOND, 0);
        sb.append(" AND time > ?");
        args.add(String.valueOf(currentDay.getTimeInMillis()));

        try {
            results = dao.queryRaw(sb.toString(), args.toArray(new String[args.size()]));
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
        EntityDao<Operation> dao = DbProvider.getHelper().getEntityDao(Operation.class);
        StringBuilder sb = new StringBuilder("SELECT SUM(amount) FROM operation WHERE category_id = ?");
        List<String> args = new ArrayList<>();
        args.add(category.getId().toString());
        
        if(budget.getCoveredAccount() != null) {
            sb.append(" AND orderer_id = ?");
            args.add(budget.getCoveredAccount().getId().toString());
        }
        
        if(budget.getStartTime() != null) {
            sb.append(" AND time > ?");
            args.add(String.valueOf(budget.getStartTime().getTime()));
        }

        if(budget.getEndTime() != null) {
            sb.append(" AND time < ?");
            args.add(String.valueOf(budget.getEndTime().getTime()));
        }
        
        try {
            results = dao.queryRaw(sb.toString(), args.toArray(new String[args.size()]));
            String[] res = results.getFirstResult();
            sum = res[0] == null ? BigDecimal.ZERO : new BigDecimal(res[0]);
            results.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sum;
    }
}
