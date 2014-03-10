package com.adonai.wallet;

import android.support.v4.app.DialogFragment;

/**
 * Created by adonai on 10.03.14.
 */
public class WalletBaseDialogFragment extends DialogFragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }
}
