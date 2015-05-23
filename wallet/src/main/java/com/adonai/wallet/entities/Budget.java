package com.adonai.wallet.entities;

import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity representing a budget
 * <br/>
 * Budget may have child {@link BudgetItem}'s that track progress and expiration
 * and may specify account that restricts child budget items' counters to its expenses only.
 * <br/>
 * Budgets may also have flags that describe its behaviour and expiration conditions
 *
 * <br/><br/>
 * For now budgets are local items not being synchronized with server
 *
 * @see com.adonai.wallet.entities.BudgetItem
 * @author Adonai
 */
@DatabaseTable(daoClass = EntityDao.class)
public class Budget extends Entity {

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(columnName = "covered_account", foreign = true, foreignAutoRefresh = true)
    private Account coveredAccount;

    @ForeignCollectionField
    private ForeignCollection<BudgetItem> content;

    @DatabaseField(columnName = "start_time", canBeNull = false, dataType = DataType.DATE_LONG)
    private Date startTime;

    @DatabaseField(columnName = "end_time", canBeNull = false, dataType = DataType.DATE_LONG)
    private Date endTime;

    @DatabaseField
    private long flags; // primitive: zero if null in DB

    @DatabaseField(columnName = "repeat_time_seconds")
    private Long repeatTimeSeconds;

    @DatabaseField(columnName = "warning_amount")
    private BigDecimal warningAmount;

    @DatabaseField(columnName = "max_amount")
    private BigDecimal maxAmount; // maximum amount for the whole budget

    @DatabaseField(columnName = "max_daily_amount")
    private BigDecimal maxDailyAmount; // maximum amount for a day

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Account getCoveredAccount() {
        return coveredAccount;
    }

    public void setCoveredAccount(Account coveredAccount) {
        this.coveredAccount = coveredAccount;
    }

    public ForeignCollection<BudgetItem> getContent() {
        return content;
    }

    public void setContent(ForeignCollection<BudgetItem> content) {
        this.content = content;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Budget)) return false;

        final Budget budget = (Budget) o;

        if (coveredAccount != null ? !coveredAccount.equals(budget.coveredAccount) : budget.coveredAccount != null)
            return false;
        if (!endTime.equals(budget.endTime))
            return false;
        if (!name.equals(budget.name))
            return false;
        if (!startTime.equals(budget.startTime))
            return false;

        return true;
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    public Long getRepeatTimeSeconds() {
        return repeatTimeSeconds;
    }

    public void setRepeatTimeSeconds(Long repeatTimeSeconds) {
        this.repeatTimeSeconds = repeatTimeSeconds;
    }

    public BigDecimal getWarningAmount() {
        return warningAmount;
    }

    public void setWarningAmount(BigDecimal warningAmount) {
        this.warningAmount = warningAmount;
    }

    public BigDecimal getMaxAmount() {
        if(maxAmount != null)
            return maxAmount;
        
        BigDecimal result = BigDecimal.ZERO;
        ForeignCollection<BudgetItem> budgets = getContent();
        if(budgets == null || budgets.isEmpty())
            return result;

        for(BudgetItem bi : budgets) {
            result = result.add(bi.getMaxAmount());
        }

        return result;
    }
    
    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        ForeignCollection<BudgetItem> budgets = getContent();
        if(budgets == null || budgets.isEmpty())
            return result;

        for(BudgetItem bi : budgets) {
            result = result.add(bi.getProgress());
        }

        return result;
    }
    
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
    
    public boolean hasExplicitMaxAmount() {
        return maxAmount != null;
    }

    public BigDecimal getMaxDailyAmount() {
        return maxDailyAmount;
    }

    public void setMaxDailyAmount(BigDecimal maxDailyAmount) {
        this.maxDailyAmount = maxDailyAmount;
    }

    public enum Flags {
        REPEATING                       (0x1),             // repeatable budgets (daily, weekly, monthly)
        AUTO_EXPANDING                  (0x2),        // auto-expanding budgets (child items automatically match the size)
        WARN_ON_OVERFLOW                (0x4),      // print a warning to user when overflown
        WARN_ON_NEAR_BREACH             (0x8);   // print a warning to user when almost overflown

        private int value;

        Flags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
