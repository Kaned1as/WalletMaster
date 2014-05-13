package com.adonai.wallet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.adonai.wallet.sync.SyncStateMachine;

/**
 * @author adonai
 */
public class WalletBaseActivity extends Activity implements SyncStateMachine.SyncListener {

    protected DatabaseDAO mEntityDAO;
    protected SyncStateMachine mSyncMachine;
    protected ProgressDialog mProgressDialog;
    protected SharedPreferences mPreferences;
    protected Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add all the currencies that are stored within mEntityDAO
        mEntityDAO = new DatabaseDAO(this);
        mSyncMachine = new SyncStateMachine(this);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mHandler = new Handler(new SyncCallback());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEntityDAO.close();
        mSyncMachine.shutdown();
    }

    public DatabaseDAO getEntityDAO() {
        return mEntityDAO;
    }

    public SyncStateMachine getSyncMachine() {
        return mSyncMachine;
    }

    public void startSync() {
        mProgressDialog.setTitle(R.string.sync_starting);
        mProgressDialog.setMessage(getString(R.string.authenticating));
        mProgressDialog.show();
        mSyncMachine.setState(SyncStateMachine.State.AUTH);
    }

    @Override
    public void handleSyncMessage(SyncStateMachine.State what, String errorMsg) {
        mHandler.sendMessage(mHandler.obtainMessage(what.ordinal(), errorMsg));
    }

    private class SyncCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            final SyncStateMachine.State state = SyncStateMachine.State.values()[msg.what];
            switch (state) {
                case INIT:
                    mProgressDialog.hide();
                    // show message (error or success)
                    Toast.makeText(WalletBaseActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    return true;
                case AUTH_ACK:
                    mProgressDialog.setMessage(getString(R.string.getting_accounts));
                    return true;
                case AUTH_DENIED:
                    mProgressDialog.hide();
                    return true;
                case ACC_REQ_SENT:
                    mProgressDialog.setMessage(getString(R.string.account_request_sent));
                    return true;
                case ACC_REQ_ACK:
                    mProgressDialog.setMessage(getString(R.string.account_response_received));
                    return true;
            }
            return false;
        }
    }
}
