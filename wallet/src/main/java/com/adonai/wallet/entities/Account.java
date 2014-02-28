package com.adonai.wallet.entities;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by adonai on 22.02.14.
 */
public class Account {
    private Long id;
    private String name;
    private String description;
    private Currency currency;
    private BigDecimal amount;
    private List<Operation> operations; // foreign key from operations to budget (OneToMany)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }
}
