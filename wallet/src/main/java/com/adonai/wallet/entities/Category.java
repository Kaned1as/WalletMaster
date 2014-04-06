package com.adonai.wallet.entities;

/**
 * Created by adonai on 23.02.14.
 */
public class Category {

    final public static int EXPENSE = 0;
    final public static int INCOME = 1;

    private long id;
    private String name;
    private int type;
    private Account preferredAccount;
    private Long guid;

    public Category() {
    }

    public Category(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Account getPreferredAccount() {
        return preferredAccount;
    }

    public void setPreferredAccount(Account preferredAccount) {
        this.preferredAccount = preferredAccount;
    }

    public Long getGuid() {
        return guid;
    }

    public void setGuid(Long guid) {
        this.guid = guid;
    }
}
