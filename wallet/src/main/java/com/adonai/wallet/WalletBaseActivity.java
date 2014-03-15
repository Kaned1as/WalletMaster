package com.adonai.wallet;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

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
