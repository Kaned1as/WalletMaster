package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.adonai.wallet.sync.SyncStateMachine;

import static com.adonai.wallet.WalletConstants.ACCOUNT_NAME_KEY;
import static com.adonai.wallet.WalletConstants.ACCOUNT_PASSWORD_KEY;
import static com.adonai.wallet.WalletConstants.ACCOUNT_SYNC_KEY;

/**
 * Dialog fragment for authorizing/registering account for the sync
 *
 * @author adonai
 */
public class SyncDialogFragment extends WalletBaseDialogFragment implements DialogInterface.OnClickListener, SyncStateMachine.SyncListener {

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
        builder.setPositiveButton(R.string.confirm, this);

        getWalletActivity().getSyncMachine().registerObserver(this);
        return builder.create();
    }

    @Override
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
    public void dismiss() {
        getWalletActivity().getSyncMachine().unregisterObserver(this);
        super.dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit()
            .putString(ACCOUNT_NAME_KEY, mAccountName.getText().toString())
            .putString(ACCOUNT_PASSWORD_KEY, mAccountPassword.getText().toString())
            .putBoolean(ACCOUNT_SYNC_KEY, mSyncType.getCheckedRadioButtonId() == R.id.already_have_radio)
            .commit();


        getWalletActivity().startSync();
    }
}
