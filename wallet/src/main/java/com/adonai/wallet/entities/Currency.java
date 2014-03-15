package com.adonai.wallet.entities;

/**
 * @author adonai
 */
public class Currency {
    public Currency(String code, String description, String usedIn) {
        assert code != null;

        this.code = code;
        this.description = description;
        this.usedIn = usedIn;
    }

    private String code;
    private String description;
    private String usedIn;

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
