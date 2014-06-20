package com.adonai.wallet.entities;

import com.adonai.wallet.DatabaseDAO;

import java.util.Date;
import java.util.List;

/**
 * Created by adonai on 19.06.14.
 */
@EntityDescriptor(type = DatabaseDAO.EntityType.BUDGETS)
public class Budget extends Entity {

    private String name;
    private Account coveredAccount;
    private List<BudgetItem> content;
    private Date startTime, endTime;

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

    @Override
    public String persist(DatabaseDAO dao) {
        return null;
    }

    @Override
    public int update(DatabaseDAO dao) {
        return 0;
    }

    public static Budget getFromDB(DatabaseDAO db, String id) {

    }
}
