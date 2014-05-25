package com.adonai.wallet;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by adonai on 24.05.14.
 */
public class WalletPreferencesFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

}
