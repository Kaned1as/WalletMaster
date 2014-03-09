package com.adonai.wallet;

import android.support.v4.app.Fragment;

/**
 * Created by adonai on 09.03.14.
 */
public class WalletBaseFragment extends Fragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }
}
