package com.adonai.wallet.entities;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
    private BigDecimal amountCharged;
    private Double convertingRate;
    private Category category;

    public Operation() {
    }

    public Operation(Account charger, BigDecimal amountCharged, Category category) {
        this.charger = charger;
        this.amountCharged = amountCharged;
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

    public BigDecimal getAmountCharged() {
        return amountCharged;
    }

    public void setAmountCharged(BigDecimal amountCharged) {
        this.amountCharged = amountCharged;
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

    public void setTime(Date time) {
        if(this.time == null)
            this.time = Calendar.getInstance();
        this.time.setTime(time);
    }

    public String getTimeString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(this.time.getTime());
    }

    public OperationType getOperationType() {
        if(getBeneficiar() != null)
            return OperationType.TRANSFER;

        if(getCategory().getType() == Category.EXPENSE)
            return OperationType.EXPENSE;

        return OperationType.INCOME;
    }
}
