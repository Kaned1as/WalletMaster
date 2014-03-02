package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
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
    private Spinner mColorSelector;
    private EditText mInitialAmount;

    private Account mAccount;

    public CreateAccountDialogFragment() {
        super();
    }

    public CreateAccountDialogFragment(Account toModify) {
        super();

        if(toModify != null)
            mAccount = toModify;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.account_create_dialog, null);
        assert dialog != null;

        mAccountName = (EditText) dialog.findViewById(R.id.name_edit);
        mAccountDescription = (EditText) dialog.findViewById(R.id.description_edit);
        mCurrencySelector = (Spinner) dialog.findViewById(R.id.currency_spinner);
        mColorSelector = (Spinner) dialog.findViewById(R.id.color_spinner);
        mInitialAmount = (EditText) dialog.findViewById(R.id.initial_amount_edit);
        mCreateButton = (Button) dialog.findViewById(R.id.create_modify_account_button);

        CurrencyAdapter adapter = new CurrencyAdapter(getActivity(), android.R.layout.simple_spinner_item, Currency.getAvailableCurrencies());
        mCurrencySelector.setAdapter(adapter);
        mCreateButton.setOnClickListener(this);
        ColorSpinnerAdapter colorAdapter = new ColorSpinnerAdapter(getActivity(), getResources().getStringArray(R.array.colors));
        mColorSelector.setAdapter(colorAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(mAccount != null) {
            mCreateButton.setText(R.string.edit);
            builder.setTitle(R.string.edit_account).setView(dialog);

            mAccountName.setText(mAccount.getName());
            mAccountDescription.setText(mAccount.getDescription());
            mCurrencySelector.setSelection(adapter.getPosition(mAccount.getCurrency()));
            mColorSelector.setSelection(colorAdapter.getPosition(String.format("#%06X", (0xFFFFFF & mAccount.getColor()))));
            mInitialAmount.setText(mAccount.getAmount().toPlainString());
        } else {
            builder.setTitle(R.string.create_new_account).setView(dialog);
            mCurrencySelector.setSelection(adapter.getPosition(Currency.getCurrencyForCode("RUB")));
            mInitialAmount.setText("0.0");
        }




        return builder.create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_modify_account_button: {
                if(mAccountName.getText() == null || mAccountName.getText().toString().equals("")) {
                    Toast.makeText(getActivity(), R.string.account_name_invalid, Toast.LENGTH_SHORT).show();
                    break;
                }
                if(mAccount != null)  {
                    mAccount.setName(mAccountName.getText().toString());
                    mAccount.setDescription(mAccountDescription.getText().toString());
                    mAccount.setCurrency((Currency) mCurrencySelector.getSelectedItem());
                    mAccount.setAmount(Utils.getValue(mInitialAmount.getText().toString(), BigDecimal.ZERO));
                    mAccount.setColor(Color.parseColor((String) mColorSelector.getSelectedItem()));

                    int result = ((WalletBaseActivity) getActivity()).getEntityDAO().updateAccount(mAccount);
                    if(result > 0)
                        dismiss();
                    else
                        Toast.makeText(getActivity(), R.string.account_not_found, Toast.LENGTH_SHORT).show();
                } else {
                    mAccount = new Account();
                    mAccount.setName(mAccountName.getText().toString());
                    mAccount.setDescription(mAccountDescription.getText().toString());
                    mAccount.setCurrency((Currency) mCurrencySelector.getSelectedItem());
                    mAccount.setAmount(Utils.getValue(mInitialAmount.getText().toString(), BigDecimal.ZERO));
                    mAccount.setColor(Color.parseColor((String) mColorSelector.getSelectedItem()));

                    long insertRes = ((WalletBaseActivity) getActivity()).getEntityDAO().addAccount(mAccount);
                    if(insertRes != -1)
                        dismiss();
                    else
                        Toast.makeText(getActivity(), R.string.account_already_exist, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    public class ColorSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
        public ColorSpinnerAdapter(Context context, String[] objects) {
            super(context, R.layout.color_list_item, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            if (rowView == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                rowView = inflater.inflate(R.layout.color_list_item, null);
                rowView.findViewById(R.id.color_view).setBackgroundColor(Color.parseColor(getItem(position)));
            } else
                rowView.findViewById(R.id.color_view).setBackgroundColor(Color.parseColor(getItem(position)));
            return rowView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                //Color
                // Get a new instance of the row layout view
                LayoutInflater inflater = getActivity().getLayoutInflater();
                rowView = inflater.inflate(R.layout.color_list_item, null);
                rowView.findViewById(R.id.color_view).setBackgroundColor(Color.parseColor(getItem(position)));
            } else
                rowView.findViewById(R.id.color_view).setBackgroundColor(Color.parseColor(getItem(position)));
            return rowView;
        }
    }

    public class CurrencyAdapter extends ArrayAdapter<Currency> implements SpinnerAdapter {

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
