package com.adonai.wallet.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

/**
 * Sync adapter to use with {@link WalletSyncService}
 * Performs syncing of data between linked devices and server database
 * 
 * @author Adonai
 */
public class WalletSyncAdapter extends AbstractThreadedSyncAdapter {
    
    public WalletSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public WalletSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        syncResult.fullSyncRequested = true;
        SyncStateMachine stateMachine = new SyncStateMachine(getContext());
        stateMachine.setState(SyncStateMachine.State.SYNC_START);
    }
}
