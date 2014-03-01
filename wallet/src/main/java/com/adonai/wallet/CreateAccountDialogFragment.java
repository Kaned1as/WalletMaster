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
    private Button mCreateButton;
    private EditText mAccountName;
    private EditText mAccountDescription;
    private Spinner mCurrencySelector;
    private EditText mInitialAmount;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.account_create_dialog, null);
        assert dialog != null;

        mAccountName = (EditText) dialog.findViewById(R.id.name_edit);
        mAccountDescription = (EditText) dialog.findViewById(R.id.description_edit);
        mCurrencySelector = (Spinner) dialog.findViewById(R.id.currency_spinner);
        mInitialAmount = (EditText) dialog.findViewById(R.id.initial_amount_edit);
        mCreateButton = (Button) dialog.findViewById(R.id.create_account_button);

        CurrencyAdapter adapter = new CurrencyAdapter(getActivity(), android.R.layout.simple_spinner_item, Currency.getAvailableCurrencies());
        mCurrencySelector.setAdapter(adapter);
        mCurrencySelector.setSelection(adapter.getPosition(Currency.getCurrencyForCode("RUB")));
        mInitialAmount.setText("0.0");
        mCreateButton.setOnClickListener(this);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_new_account).setView(dialog);

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_account_button: {
                if(mAccountName.getText() == null || mAccountName.getText().equals("")) {
                    Toast.makeText(getActivity(), R.string.account_name_invalid, Toast.LENGTH_SHORT).show();
                    break;
                }

                final Account acc = new Account();
                acc.setName(mAccountName.getText().toString());
                acc.setDescription(mAccountDescription.getText().toString());
                acc.setCurrency((Currency) mCurrencySelector.getSelectedItem());
                acc.setAmount(Utils.getValue(mInitialAmount.getText().toString(), BigDecimal.ZERO));

                long insertRes = ((WalletBaseActivity) getActivity()).getEntityDAO().addAccount(acc);
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
