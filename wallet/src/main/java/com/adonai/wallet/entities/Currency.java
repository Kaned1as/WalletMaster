package com.adonai.wallet.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author adonai
 */
public class Currency {

    private static Map<String, Currency> currenciesMap = new LinkedHashMap<>();

    // static initializer - see file currencies.csv in assets folder
    // fills in currenciesMap
    static {
        InputStream allCurrencies = Currency.class.getResourceAsStream("/assets/currencies.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(allCurrencies));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(":");
                switch (tokens.length) {
                    case 3: currenciesMap.put(tokens[0], new Currency(tokens[0], tokens[1], tokens[2])); break;
                    case 2: currenciesMap.put(tokens[0], new Currency(tokens[0], tokens[1], null)); break;
                    case 1: currenciesMap.put(tokens[0], new Currency(tokens[0], null, null)); break;
                }
            }
        } catch (IOException e) {
            currenciesMap.clear();
        }
    }

    public static void addCustomCurrency(String code, String description, String usedIn) {
        assert code != null;
        currenciesMap.put(code, new Currency(code, description, usedIn));
    }

    public static void addCustomCurrency(Currency curr) {
        currenciesMap.put(curr.getCode(), curr);
    }

    public static Currency getCurrencyForCode(String code) {
        return currenciesMap.get(code);
    }

    public static List<Currency> getAvailableCurrencies() {
        return new ArrayList<>(currenciesMap.values());
    }

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

    public void setCode(String code) {
        this.code = code;
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
}
