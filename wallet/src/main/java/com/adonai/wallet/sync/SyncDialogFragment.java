package com.adonai.wallet.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.adonai.wallet.R;
import com.adonai.wallet.WalletBaseDialogFragment;
import com.adonai.wallet.WalletConstants;
import com.adonai.wallet.sync.SyncStateMachine;

import java.util.concurrent.TimeUnit;

import static com.adonai.wallet.WalletConstants.*;
import static com.adonai.wallet.WalletConstants.ACCOUNT_NAME_KEY;
import static com.adonai.wallet.WalletConstants.ACCOUNT_PASSWORD_KEY;
import static com.adonai.wallet.WalletConstants.ACCOUNT_SYNC_KEY;

/**
 * Dialog fragment for authorizing/registering account for the sync
 *
 * @author adonai
 */
public class SyncDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {

    private RadioGroup mSyncType;
    private EditText mAccountName;
    private EditText mAccountPassword;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.sync_dialog, null);
        assert dialog != null;

        mSyncType = (RadioGroup) dialog.findViewById(R.id.sync_type_switch);
        mAccountName = (EditText) dialog.findViewById(R.id.account_name_edit);
        mAccountPassword = (EditText) dialog.findViewById(R.id.account_password_edit);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);
        builder.setPositiveButton(R.string.confirm, null);

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    //@Override
    public void handleSyncMessage(SyncStateMachine.State what, String errorMsg) {
        switch (what) {
            case AUTH_ACK:
                dismiss();
                break;
            case AUTH_DENIED:
                Toast.makeText(getActivity(), errorMsg, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @SuppressWarnings("deprecation") // accountManager.removeAccount
    @Override
    public void onClick(View button) {
        String accountName = mAccountName.getText().toString();
        String accountPassword = mAccountPassword.getText().toString();
        Boolean isSynced = mSyncType.getCheckedRadioButtonId() == R.id.already_have_radio;

        // Create the account type and default account
        Account newAccount = new Account(accountName, ACCOUNT_TYPE);
        Bundle properties = new Bundle();
        properties.putString(WalletConstants.ACCOUNT_SYNC_KEY, isSynced.toString());
        
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
        Account[] associatedAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        for(Account acc : associatedAccounts) {
            accountManager.removeAccount(acc, null, null); // clear all previous accounts
        }
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, accountPassword, properties)) {
            // success, let's start sync
            Bundle syncProperties = new Bundle(2);
            syncProperties.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // request manual sync
            syncProperties.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // start immediately
            
            ContentResolver.requestSync(newAccount, SYNC_AUTHORITY, syncProperties); // sync now!
            ContentResolver.addPeriodicSync(newAccount, SYNC_AUTHORITY, Bundle.EMPTY, TimeUnit.MINUTES.toSeconds(30));
            
            dismiss();
        } else {
            Toast.makeText(getActivity(), R.string.cannot_create_account, Toast.LENGTH_LONG).show();
        }
    }
}
