package com.adonai.wallet.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by adonai on 19.06.14.
 */
@DatabaseTable
public class Budget {

    @DatabaseField(id = true)
    private UUID id = UUID.randomUUID();

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(foreign = true)
    private Account coveredAccount;

    @ForeignCollectionField
    private List<BudgetItem> content;

    @DatabaseField(canBeNull = false)
    private Date startTime;

    @DatabaseField(canBeNull = false)
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

    public List<BudgetItem> getContent() {
        return content;
    }

    public void setContent(List<BudgetItem> content) {
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
