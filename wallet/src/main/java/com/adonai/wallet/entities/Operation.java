package com.adonai.wallet.entities;

import com.adonai.wallet.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by adonai on 23.02.14.
 */
public class Operation {

    public enum OperationType {
        EXPENSE,
        INCOME,
        TRANSFER
    }

    private Long id;
    private String description;
    private Calendar time;
    private Account charger;
    private Account beneficiar;
    private BigDecimal amount;
    private Double convertingRate;
    private Category category;
    private Long guid;

    public Operation() {
    }

    public Operation(BigDecimal amount, Category category) {
        this.amount = amount;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getCharger() {
        return charger;
    }

    public void setCharger(Account charger) {
        this.charger = charger;
    }

    public Account getBeneficiar() {
        return beneficiar;
    }

    public void setBeneficiar(Account beneficiar) {
        this.beneficiar = beneficiar;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Double getConvertingRate() {
        return convertingRate;
    }

    public void setConvertingRate(Double convertingRate) {
        this.convertingRate = convertingRate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public Long getGuid() {
        return guid;
    }

    public void setGuid(Long guid) {
        this.guid = guid;
    }

    public void setTime(Date time) {
        if(this.time == null)
            this.time = Calendar.getInstance();
        this.time.setTime(time);
    }

    public String getTimeString() {
        return Utils.SQLITE_DATE_FORMAT.format(this.time.getTime());
    }

    public OperationType getOperationType() {
        if(getBeneficiar() != null && getCharger() != null)
            return OperationType.TRANSFER;

        if(getCategory().getType() == Category.EXPENSE)
            return OperationType.EXPENSE;

        return OperationType.INCOME;
    }

    public BigDecimal getAmountDelivered() {
        if(getConvertingRate() != null)
           return getAmount().divide(BigDecimal.valueOf(getConvertingRate()), 2, RoundingMode.HALF_UP);
        else
            return getAmount();
    }
}
