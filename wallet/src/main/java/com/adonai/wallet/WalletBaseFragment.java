package com.adonai.wallet;


import android.app.Fragment;

/**
 * @author adonai
 */
public abstract class WalletBaseFragment extends Fragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }
}
