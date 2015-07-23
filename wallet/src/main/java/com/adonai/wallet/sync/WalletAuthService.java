package com.adonai.wallet.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A bound Service that instantiates the authenticator
 * when started.
 * Mandatory as Android Framework requires it.
 *
 * @author Adonai
 * @author Android Developers
 */
public class WalletAuthService extends Service {
    
    // Instance field that stores the authenticator object
    private WalletAccountAuthenticator mAuthenticator;
    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new WalletAccountAuthenticator(this);
    }
    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
