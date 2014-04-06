package com.adonai.wallet.entities;

import com.adonai.wallet.sync.SyncProtocol;

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
    private Integer color;
    private Long guid;
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

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Long getGuid() {
        return guid;
    }

    public void setGuid(Long guid) {
        this.guid = guid;
    }

    public static Account fromProtoAccount(SyncProtocol.Account account) {
        final Account temp = new Account();
        temp.setName(account.getName());
        temp.setAmount(new BigDecimal(account.getAmount()));
        temp.setColor(account.getColor());
        temp.setDescription(account.getDescription());
        temp.setCurrency(new Currency(account.getCurrency()));
        temp.setGuid(account.getID()); // ID of synced account is real ID in server database
        return temp;
    }

    public static SyncProtocol.Account toProtoAccount(Account account) {
        return SyncProtocol.Account.newBuilder()
                .setID(account.getId())
                .setName(account.getName())
                .setAmount(account.getAmount().toPlainString())
                .setColor(account.getColor())
                .setDescription(account.getDescription())
                .setCurrency(account.getCurrency().getCode())
                .build();
    }
}
