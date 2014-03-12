package com.adonai.wallet.entities;

/**
 * Created by adonai on 23.02.14.
 */
public class Category {

    public enum CategoryType {
        EXPENSE,
        INCOME
    }

    private String name;
    private CategoryType type;
    private Account preferredAccount;

    public Category() {
    }

    public Category(String name, CategoryType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public Account getPreferredAccount() {
        return preferredAccount;
    }

    public void setPreferredAccount(Account preferredAccount) {
        this.preferredAccount = preferredAccount;
    }
}
