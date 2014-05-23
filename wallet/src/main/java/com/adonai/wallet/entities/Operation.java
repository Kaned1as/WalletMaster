package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.sync.SyncProtocol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;

@EntityDescriptor(type = DatabaseDAO.EntityType.OPERATIONS)
public class Operation extends Entity {

    public enum OperationType {
        EXPENSE,
        INCOME,
        TRANSFER
    }

    private String description;
    private Date time;
    private Account charger;
    private Account beneficiar;
    private BigDecimal amount;
    private Double convertingRate;
    private Category category;

    public Operation() {
    }

    public Operation(BigDecimal amount, Category category) {
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

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
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
    public String persist(DatabaseDAO dao) {
        Log.d("addOperation", getAmount().toPlainString());

        final ContentValues values = new ContentValues(8);
        if(getId() != null) // use with caution
            values.put(DatabaseDAO.OperationsFields._id.toString(), getId());
        else
            values.put(DatabaseDAO.OperationsFields._id.toString(), UUID.randomUUID().toString());

        values.put(DatabaseDAO.OperationsFields.DESCRIPTION.toString(), getDescription());
        values.put(DatabaseDAO.OperationsFields.CATEGORY.toString(), getCategory().getId());        // mandatory
        values.put(DatabaseDAO.OperationsFields.AMOUNT.toString(), getAmount().toPlainString());    // mandatory
        values.put(DatabaseDAO.OperationsFields.TIME.toString(), getTime().getTime());      // mandatory

        if(getCharger() != null)
            values.put(DatabaseDAO.OperationsFields.CHARGER.toString(), getCharger().getId());
        if(getBeneficiar() != null)
            values.put(DatabaseDAO.OperationsFields.RECEIVER.toString(), getBeneficiar().getId());
        if(getConvertingRate() != null)
            values.put(DatabaseDAO.OperationsFields.CONVERT_RATE.toString(), getConvertingRate());

        long row = dao.insert(values, entityType.toString());
        if(row > 0)
            return values.getAsString(DatabaseDAO.OperationsFields._id.toString());
        else
            return null;
    }

    @Override
    public int update(DatabaseDAO dao) {
        // 2. create ContentValues to add key "column"/value
        final ContentValues values = new ContentValues();
        values.put(DatabaseDAO.OperationsFields.DESCRIPTION.toString(), getDescription());
        values.put(DatabaseDAO.OperationsFields.TIME.toString(), getTime().getTime());                  // mandatory
        values.put(DatabaseDAO.OperationsFields.AMOUNT.toString(), getAmount().toPlainString());    // mandatory
        values.put(DatabaseDAO.OperationsFields.CATEGORY.toString(), getCategory().getId());        // mandatory


        if(getCharger() != null)
            values.put(DatabaseDAO.OperationsFields.CHARGER.toString(), getCharger().getId());
        else
            values.put(DatabaseDAO.OperationsFields.CHARGER.toString(), (String) null);
        if(getBeneficiar() != null)
            values.put(DatabaseDAO.OperationsFields.RECEIVER.toString(), getBeneficiar().getId());
        else
            values.put(DatabaseDAO.OperationsFields.RECEIVER.toString(), (String) null);

        if(getConvertingRate() != null)
            values.put(DatabaseDAO.OperationsFields.CONVERT_RATE.toString(), getConvertingRate());
        else
            values.put(DatabaseDAO.OperationsFields.CONVERT_RATE.toString(), (String) null);

        return dao.update(values, entityType.toString());
    }

    public static Operation getFromDB(DatabaseDAO dao, String id) {
        final Long preciseTime = System.currentTimeMillis();
        final Cursor cursor = dao.get(DatabaseDAO.EntityType.OPERATIONS, id);
        if (cursor.moveToFirst()) {
            final Operation op = new Operation();
            op.setId(cursor.getString(DatabaseDAO.OperationsFields._id.ordinal()));
            op.setDescription(cursor.getString(DatabaseDAO.OperationsFields.DESCRIPTION.ordinal()));
            op.setCategory(Category.getFromDB(dao, cursor.getString(DatabaseDAO.OperationsFields.CATEGORY.ordinal())));
            op.setTime(new Date(cursor.getLong(DatabaseDAO.OperationsFields.TIME.ordinal())));
            if(!cursor.isNull(DatabaseDAO.OperationsFields.CHARGER.ordinal()))
                op.setCharger(Account.getFromDB(dao, cursor.getString(DatabaseDAO.OperationsFields.CHARGER.ordinal())));
            if(!cursor.isNull(DatabaseDAO.OperationsFields.RECEIVER.ordinal()))
                op.setBeneficiar(Account.getFromDB(dao, cursor.getString(DatabaseDAO.OperationsFields.RECEIVER.ordinal())));
            op.setAmount(new BigDecimal(cursor.getString(DatabaseDAO.OperationsFields.AMOUNT.ordinal())));
            if(!cursor.isNull(DatabaseDAO.OperationsFields.CONVERT_RATE.ordinal()))
                op.setConvertingRate(cursor.getDouble(DatabaseDAO.OperationsFields.CONVERT_RATE.ordinal()));
            cursor.close();

            Log.d(String.format("getOperation(%s), took %d ms", id, System.currentTimeMillis() - preciseTime), op.getAmount().toPlainString());
            return op;
        }

        cursor.close();
        return null;
    }

    public static Operation fromProtoOperation(SyncProtocol.Operation operation, DatabaseDAO dao) {
        final Operation temp = new Operation();
        temp.setId(operation.getID());
        temp.setDescription(operation.getDescription());
        temp.setCategory(Category.getFromDB(dao, operation.getCategoryId()));
        temp.setTime(new Date(operation.getTime()));
        if(operation.hasChargerId())
            temp.setCharger(Account.getFromDB(dao, operation.getChargerId()));
        if(operation.hasBeneficiarId())
            temp.setBeneficiar(Account.getFromDB(dao, operation.getBeneficiarId()));
        temp.setAmount(new BigDecimal(operation.getAmount()));
        if(operation.hasConvertingRate())
            temp.setConvertingRate(operation.getConvertingRate());
        return temp;
    }

    public static SyncProtocol.Operation toProtoOperation(Operation operation) {
        final SyncProtocol.Operation.Builder builder = SyncProtocol.Operation.newBuilder()
                .setID(operation.getId())
                .setDescription(operation.getDescription())
                .setAmount(operation.getAmount().toPlainString())
                .setTime(operation.getTime().getTime())
                .setCategoryId(operation.getCategory().getId());
        if(operation.getCharger() != null)
            builder.setChargerId(operation.getCharger().getId());
        if(operation.getBeneficiar() != null)
            builder.setBeneficiarId(operation.getBeneficiar().getId());
        if(operation.getConvertingRate() != null)
            builder.setConvertingRate(operation.getConvertingRate());

        return builder.build();
    }
}
