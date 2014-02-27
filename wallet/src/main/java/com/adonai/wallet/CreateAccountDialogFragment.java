package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.adonai.wallet.entities.Account;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * @author adonai
 */
public class CreateAccountDialogFragment extends DialogFragment implements View.OnClickListener {
    Button createButton;
    EditText accountName;
    EditText accountDescription;
    Spinner currencySelector;
    EditText initialAmount;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.account_create_dialog, null);
        assert dialog != null;

        accountName = (EditText) dialog.findViewById(R.id.name_edit);
        accountDescription = (EditText) dialog.findViewById(R.id.description_edit);
        currencySelector = (Spinner) dialog.findViewById(R.id.currency_spinner);
        initialAmount = (EditText) dialog.findViewById(R.id.initial_amount_edit);
        createButton = (Button) dialog.findViewById(R.id.create_account_button);

        createButton.setOnClickListener(this);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_new_account).setView(dialog);

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_account_button:
                final Account acc = new Account();
                acc.setName(accountName.getText().toString());
                acc.setDescription(accountDescription.getText().toString());
                acc.setCurrency(Currency.getInstance(currencySelector.getSelectedItem().toString()));
                acc.setAmount(new BigDecimal(initialAmount.getText().toString()));
                dismiss();
                break;
        }
    }
}
