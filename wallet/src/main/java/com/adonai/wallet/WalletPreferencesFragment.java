package com.adonai.wallet;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.CompoundButton;

/**
 * Dialog fragment showing preferences editing form
 *
 * @author adonai
 */
public class WalletPreferencesFragment extends PreferenceFragment {

    public static final String ASK_FOR_DELETE = "ask.for.delete";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    public static class DontAskForDelete implements CompoundButton.OnCheckedChangeListener {

        private final WalletBaseActivity mActivity;

        public DontAskForDelete(WalletBaseActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) // do not show this dialog again
                mActivity.getPreferences().edit().putBoolean(ASK_FOR_DELETE, false).commit();
            else
                mActivity.getPreferences().edit().putBoolean(ASK_FOR_DELETE, true).commit();
        }
    }
}
