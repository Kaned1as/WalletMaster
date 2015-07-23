package com.adonai.wallet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.adonai.wallet.sync.SyncStateMachine;

/**
 * All wallet master activities must extend this class
 *
 * @author adonai
 */
public class WalletBaseActivity extends AppCompatActivity {

    protected ProgressDialog mProgressDialog;
    protected SharedPreferences mPreferences;
    protected Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add all the currencies that are stored within mEntityDAO
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final String theme = mPreferences.getString("app.theme", "light");
        setTheme(theme.equals("light") ? R.style.Light : R.style.Dark);

        mHandler = new Handler(new SyncCallback());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressDialog.dismiss();
    }

    /**
    public void startSync() {
        mProgressDialog.setTitle(R.string.sync_starting);
        mProgressDialog.setMessage(getString(R.string.authenticating));
        mProgressDialog.show();
    }
     **/

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
                case CAT_REQ_SENT:
                    mProgressDialog.setMessage(getString(R.string.category_request_sent));
                    return true;
                case CAT_REQ_ACK:
                    mProgressDialog.setMessage(getString(R.string.category_response_received));
                    return true;
                case OP_REQ_SENT:
                    mProgressDialog.setMessage(getString(R.string.operation_request_sent));
                    return true;
                case OP_REQ_ACK:
                    mProgressDialog.setMessage(getString(R.string.operation_response_received));
                    return true;
            }
            return false;
        }
    }

    public SharedPreferences getPreferences() {
        return mPreferences;
    }
}
