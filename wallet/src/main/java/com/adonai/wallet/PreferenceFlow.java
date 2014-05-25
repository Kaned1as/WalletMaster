package com.adonai.wallet;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Preference activity responsible for user settings handling
 * For now just loads single preference fragment
 *
 * @author Adonai
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
