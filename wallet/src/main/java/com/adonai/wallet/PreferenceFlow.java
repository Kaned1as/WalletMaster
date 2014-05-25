package com.adonai.wallet;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by adonai on 25.05.14.
 */
public class PreferenceFlow extends PreferenceActivity {

    WalletPreferencesFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preference_flow);

        mFragment = (WalletPreferencesFragment) getFragmentManager().findFragmentById(R.id.preference_fragment);
    }
}
