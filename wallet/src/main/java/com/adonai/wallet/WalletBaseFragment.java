package com.adonai.wallet;


import android.app.Fragment;

/**
 * All wallet master fragments must extend this class
 *
 * @author adonai
 */
public abstract class WalletBaseFragment extends Fragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }
}
