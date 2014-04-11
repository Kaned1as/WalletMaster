package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by adonai on 23.02.14.
 */
public class Operation extends Entity {

    public enum OperationType {
        EXPENSE,
        INCOME,
        TRANSFER
    }

    private String description;
    private Calendar time;
    private Account charger;
    private Account beneficiar;
    private BigDecimal amount;
    private Double convertingRate;
    private Category category;
    private Long guid;

    public Operation() {
        super(DatabaseDAO.EntityType.OPERATION);
    }

    public Operation(BigDecimal amount, Category category) {
        super(DatabaseDAO.EntityType.OPERATION);
        this.amount = amount;
        this.category = category;
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

    @Override
    public long persist(DatabaseDAO dao) {
        Log.d("addOperation", getAmount().toPlainString());

        final ContentValues values = new ContentValues(8);
        if(getId() != null) // use with caution
            values.put(DatabaseDAO.OperationsFields._id.toString(), getId());

        values.put(DatabaseDAO.OperationsFields.DESCRIPTION.toString(), getDescription()); // mandatory
        values.put(DatabaseDAO.OperationsFields.CATEGORY.toString(), getCategory().getId()); // mandatory
        values.put(DatabaseDAO.OperationsFields.AMOUNT.toString(), getAmount().toPlainString()); // mandatory

        if(getTime() != null)
            values.put(DatabaseDAO.OperationsFields.TIME.toString(), getTimeString());
        if(getCharger() != null)
            values.put(DatabaseDAO.OperationsFields.CHARGER.toString(), getCharger().getId());
        if(getBeneficiar() != null)
            values.put(DatabaseDAO.OperationsFields.RECEIVER.toString(), getBeneficiar().getId());
        if(getConvertingRate() != null)
            values.put(DatabaseDAO.OperationsFields.CONVERT_RATE.toString(), getConvertingRate());

        return dao.insert(values, DatabaseDAO.OPERATIONS_TABLE_NAME);
    }

    @Override
    public int update(DatabaseDAO dao) {
        // 2. create ContentValues to add key "column"/value
        final ContentValues values = new ContentValues();
        values.put(DatabaseDAO.OperationsFields.DESCRIPTION.toString(), getDescription());
        if(getTime() != null)
            values.put(DatabaseDAO.OperationsFields.TIME.toString(), getTimeString());
        values.put(DatabaseDAO.OperationsFields.CHARGER.toString(), getCharger().getId());
        if(getBeneficiar() != null)
            values.put(DatabaseDAO.OperationsFields.RECEIVER.toString(), getBeneficiar().getId());
        values.put(DatabaseDAO.OperationsFields.AMOUNT.toString(), getAmount().toPlainString());
        values.put(DatabaseDAO.OperationsFields.CONVERT_RATE.toString(), getConvertingRate());

        return dao.update(values, DatabaseDAO.OPERATIONS_TABLE_NAME);
    }

    @Override
    public int delete(DatabaseDAO dao) {
        return dao.delete(getId(), DatabaseDAO.OPERATIONS_TABLE_NAME);
    }
}
