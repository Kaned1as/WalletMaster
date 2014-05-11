package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.adonai.wallet.sync.SyncStateMachine;

import static com.adonai.wallet.WalletConstants.ACCOUNT_NAME_KEY;
import static com.adonai.wallet.WalletConstants.ACCOUNT_PASSWORD_KEY;
import static com.adonai.wallet.WalletConstants.ACCOUNT_SYNC_KEY;

/**
 * @author adonai
 */
public class SyncDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener, SyncStateMachine.SyncListener {

    private Button mConfirm;
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
        mConfirm = (Button) dialog.findViewById(R.id.account_confirm_button);
        mConfirm.setOnClickListener(this);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);

        getWalletActivity().getSyncMachine().registerObserver(this);
        return builder.create();
    }

    @Override
    public void onClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit()
                .putString(ACCOUNT_NAME_KEY, mAccountName.getText().toString())
                .putString(ACCOUNT_PASSWORD_KEY, mAccountPassword.getText().toString())
                .putBoolean(ACCOUNT_SYNC_KEY, mSyncType.getCheckedRadioButtonId() == R.id.already_have_radio)
            .commit();


        getWalletActivity().startSync();
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
        super.dismiss();
        getWalletActivity().getSyncMachine().unregisterObserver(this);
    }
}
