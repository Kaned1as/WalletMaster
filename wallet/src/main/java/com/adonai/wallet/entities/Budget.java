package com.adonai.wallet.entities;

import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by adonai on 19.06.14.
 */
@DatabaseTable(daoClass = EntityDao.class)
public class Budget extends Entity {

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Account coveredAccount;

    @ForeignCollectionField
    private ForeignCollection<BudgetItem> content;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date startTime;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date endTime;

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
}
