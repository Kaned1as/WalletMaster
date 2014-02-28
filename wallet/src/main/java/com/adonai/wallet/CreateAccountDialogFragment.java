package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Currency;

import java.math.BigDecimal;
import java.util.List;

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

        CurrencyAdapter adapter = new CurrencyAdapter(getActivity(), android.R.layout.simple_spinner_item, Currency.getAvailableCurrencies());
        currencySelector.setAdapter(adapter);
        currencySelector.setSelection(adapter.getPosition(Currency.getCurrencyForCode("RUB")));
        initialAmount.setText("0.0");
        createButton.setOnClickListener(this);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_new_account).setView(dialog);

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_account_button: {
                if(accountName.getText() == null || accountName.getText().equals("")) {
                    Toast.makeText(getActivity(), R.string.account_name_invalid, Toast.LENGTH_SHORT).show();
                    break;
                }

                final Account acc = new Account();
                acc.setName(accountName.getText().toString());
                acc.setDescription(accountDescription.getText().toString());
                acc.setCurrency((Currency) currencySelector.getSelectedItem());
                acc.setAmount(Utils.getValue(initialAmount.getText().toString(), BigDecimal.ZERO));

                long insertRes = ((WalletBaseActivity) getActivity()).getDatabase().addAccount(acc);
                if(insertRes != -1)
                    dismiss();
                else
                    Toast.makeText(getActivity(), R.string.account_already_exist, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    public class CurrencyAdapter extends ArrayAdapter<Currency> {

        public CurrencyAdapter(Context context, int resource, List<Currency> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view;
            Currency currency = getItem(position);
            if (convertView == null)
                view = View.inflate(getContext(), R.layout.currency_list_item, null);
            else
                view = convertView;

            TextView title = (TextView) view.findViewById(R.id.curr_caption_text);
            title.setText(currency.getCode());
            TextView author = (TextView) view.findViewById(R.id.curr_description_text);
            author.setText(currency.getDescription());
            TextView last_post = (TextView) view.findViewById(R.id.curr_usedin_text);
            last_post.setText(currency.getUsedIn());

            return view;
        }
    }
}
