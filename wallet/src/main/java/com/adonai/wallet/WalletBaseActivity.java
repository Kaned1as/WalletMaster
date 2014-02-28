package com.adonai.wallet;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.adonai.wallet.entities.Currency;

import java.util.List;

/**
 * Created by adonai on 28.02.14.
 */
public class WalletBaseActivity extends ActionBarActivity {

    protected DatabaseDAO database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add all the currencies that are stored within database
        database = new DatabaseDAO(this);
        final List<Currency> customCurrs = database.getCustomCurrencies();
        for(final Currency curr : customCurrs)
            Currency.addCustomCurrency(curr);
    }

    public DatabaseDAO getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseDAO database) {
        this.database = database;
    }
}
