package com.adonai.wallet.entities;

/**
 * Created by adonai on 23.02.14.
 */
public class Category {
    private String name;
    private Integer type;
    private Account preferredAccount;

    public Category() {
    }

    public Category(String name, Integer type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Account getPreferredAccount() {
        return preferredAccount;
    }

    public void setPreferredAccount(Account preferredAccount) {
        this.preferredAccount = preferredAccount;
    }
}
