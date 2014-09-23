package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.sync.SyncProtocol;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;

import static com.adonai.wallet.entities.Category.CategoryType;

/**
 * Entity representing an operation. Operations show money flow
 * across accounts and more
 * <p>
 * Required fields:
 * <ol>
 *     <li>category</li>
 *     <li>time</li>
 *     <li>amount</li>
 * </ol>
 * </p>
 * <p>
 * Optional fields:
 * <ol>
 *     <li>description</li>
 *     <li>charge account-----| choose one</li>
 *     <li>beneficiar account-| required</li>
 *     <li>conversion rate</li>
 * </ol>
 * </p>
 * @author adonai
 */
@DatabaseTable
public class Operation {

    @DatabaseField(id = true)
    private UUID id = UUID.randomUUID();

    @DatabaseField(canBeNull = false)
    private Date time;

    @DatabaseField(canBeNull = false)
    private BigDecimal amount;

    @DatabaseField(canBeNull = false)
    private Category category;

    @DatabaseField
    private String description;

    @DatabaseField(foreign = true)
    private Account orderer;

    @DatabaseField(foreign = true)
    private Account beneficiar;

    @DatabaseField
    private BigDecimal convertingRate;

    public Operation() {
    }

    public Operation(BigDecimal amount, Category category) {
        this.amount = amount;
        this.category = category;
    }

    public Account getOrderer() {
        return orderer;
    }

    public void setOrderer(Account orderer) {
        this.orderer = orderer;
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

    public BigDecimal getConvertingRate() {
        return convertingRate;
    }

    public void setConvertingRate(BigDecimal convertingRate) {
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getAmountDelivered() {
        if(getConvertingRate() != null)
           return getAmount().divide(getConvertingRate(), 2, RoundingMode.HALF_UP);
        else
            return getAmount();
    }

    public static Operation fromProtoOperation(SyncProtocol.Operation operation) {
        final Operation temp = new Operation();
        temp.setId(UUID.fromString(operation.getID()));
        temp.setDescription(operation.getDescription());
        temp.setCategory(Category.getFromDB(operation.getCategoryId()));
        temp.setTime(new Date(operation.getTime()));
        if(operation.hasChargerId())
            temp.setOrderer(Account.getFromDB(operation.getChargerId()));
        if(operation.hasBeneficiarId())
            temp.setBeneficiar(Account.getFromDB(operation.getBeneficiarId()));
        temp.setAmount(new BigDecimal(operation.getAmount()));
        if(operation.hasConvertingRate())
            temp.setConvertingRate(BigDecimal.valueOf(operation.getConvertingRate()));
        return temp;
    }

    public static SyncProtocol.Operation toProtoOperation(Operation operation) {
        final SyncProtocol.Operation.Builder builder = SyncProtocol.Operation.newBuilder()
                .setID(operation.getId().toString())
                .setDescription(operation.getDescription())
                .setAmount(operation.getAmount().toPlainString())
                .setTime(operation.getTime().getTime())
                .setCategoryId(operation.getCategory().getId().toString());
        if(operation.getOrderer() != null)
            builder.setChargerId(operation.getOrderer().getId().toString());
        if(operation.getBeneficiar() != null)
            builder.setBeneficiarId(operation.getBeneficiar().getId().toString());
        if(operation.getConvertingRate() != null)
            builder.setConvertingRate(operation.getConvertingRate().doubleValue());

        return builder.build();
    }
}
