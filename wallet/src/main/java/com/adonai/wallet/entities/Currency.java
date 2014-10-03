package com.adonai.wallet.entities;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Service class needed for conversions and account splitting
 * Note: this is not an entity. Currency tables are local for now
 * and are being filled from assets .csv file
 * <p>
 * Fields:
 * <ol>
 *     <li>code - code of currency</li>
 *     <li>description - full name</li>
 *     <li>usedIn - sample countries</li>
 * </ol>
 * @author Adonai
 */
@DatabaseTable
public class Currency {

    @DatabaseField(id = true)
    private String code;

    @DatabaseField
    private String description;

    @DatabaseField(columnName = "used_in")
    private String usedIn;

    public Currency() {
    }

    public Currency(String code) {
        this.code = code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsedIn() {
        return usedIn;
    }

    public void setUsedIn(String usedIn) {
        this.usedIn = usedIn;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Currency currency = (Currency) o;

        return code.equals(currency.code);
    }
}
