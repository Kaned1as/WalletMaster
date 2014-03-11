package com.adonai.wallet;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Currency;

import java.util.List;

/**
 * Created by adonai on 28.02.14.
 */
public class WalletBaseActivity extends ActionBarActivity {

    protected DatabaseDAO mEntityDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add all the currencies that are stored within mEntityDAO
        mEntityDAO = new DatabaseDAO(this);
        final List<Currency> customCurrs = mEntityDAO.getCustomCurrencies();
        for(final Currency curr : customCurrs)
            Currency.addCustomCurrency(curr);

        if(!mEntityDAO.hasCategories()) {
            String[] defaultOutcomeCategories = getResources().getStringArray(R.array.out_categories);
            for(String category : defaultOutcomeCategories)
                mEntityDAO.addCategory(new Category(category, 0));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEntityDAO.close();
    }

    public DatabaseDAO getEntityDAO() {
        return mEntityDAO;
    }
}
