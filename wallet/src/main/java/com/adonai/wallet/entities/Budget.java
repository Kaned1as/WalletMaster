package com.adonai.wallet.entities;

import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.adonai.wallet.DatabaseDAO.BudgetFields;
import static com.adonai.wallet.DatabaseDAO.EntityType;

/**
 * Created by adonai on 19.06.14.
 */
@EntityDescriptor(type = EntityType.BUDGETS)
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

    public static Budget getFromDB(DatabaseDAO dao, String id) {
        final Cursor cursor = dao.get(EntityType.BUDGETS, id);
        if (cursor.moveToFirst()) {
            final Budget budget = new Budget();
            budget.setId(cursor.getString(BudgetFields._id.ordinal()));
            budget.setName(cursor.getString(BudgetFields.NAME.ordinal()));
            budget.setStartTime(new Date(cursor.getLong(BudgetFields.START_TIME.ordinal())));
            budget.setEndTime(new Date(cursor.getLong(BudgetFields.END_TIME.ordinal())));
            budget.setCoveredAccount(Account.getFromDB(dao, cursor.getString(BudgetFields.COVERED_ACCOUNT.ordinal())));

            // interesting part - getting budget items
            final Cursor budgetItems = dao.getCustomCursor(EntityType.BUDGET_ITEMS, DatabaseDAO.BudgetItemFields.PARENT_BUDGET.toString(), budget.getId());
            final List<BudgetItem> dependentItems = new ArrayList<>(budgetItems.getCount());
            while (cursor.moveToNext())
                dependentItems.add(BudgetItem.getFromDB(dao, cursor.getString(DatabaseDAO.BudgetItemFields._id.ordinal())));
            budget.setContent(dependentItems);
            budgetItems.close();

            Log.d("Entity Serialization", "getBudget(" + id + "), name: " + budget.getName());
            cursor.close();
            return budget;
        }

        cursor.close();
        return null;
    }
}
