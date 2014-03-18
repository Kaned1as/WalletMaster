package com.adonai.wallet;

import android.support.v4.app.Fragment;

/**
 * @author adonai
 */
public abstract class WalletBaseFragment extends Fragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }

    public abstract void onDrawerClosed();
}
