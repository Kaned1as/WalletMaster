package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Currency;

import java.math.BigDecimal;

/**
 * @author adonai
 */
public class AccountDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {
    private Button mCreateAccount;
    private EditText mAccountName;
    private EditText mAccountDescription;
    private Spinner mCurrencySelector;
    private Spinner mColorSelector;
    private EditText mInitialAmount;

    private Account mAccount;

    public AccountDialogFragment() {
        //super();
    }

    public AccountDialogFragment(Account toModify) {
        mAccount = toModify;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.account_create_modify_dialog, null);
        assert dialog != null;

        mAccountName = (EditText) dialog.findViewById(R.id.name_edit);
        mAccountDescription = (EditText) dialog.findViewById(R.id.description_edit);
        mCurrencySelector = (Spinner) dialog.findViewById(R.id.currency_spinner);
        mColorSelector = (Spinner) dialog.findViewById(R.id.color_spinner);
        mInitialAmount = (EditText) dialog.findViewById(R.id.initial_amount_edit);
        mCreateAccount = (Button) dialog.findViewById(R.id.create_modify_account_button);

        CurrencyAdapter adapter = new CurrencyAdapter();
        mCurrencySelector.setAdapter(adapter);
        mCreateAccount.setOnClickListener(this);
        ColorSpinnerAdapter colorAdapter = new ColorSpinnerAdapter(getResources().getStringArray(R.array.colors));
        mColorSelector.setAdapter(colorAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(mAccount != null) {
            mCreateAccount.setText(R.string.confirm);
            builder.setTitle(R.string.edit_account).setView(dialog);

            mAccountName.setText(mAccount.getName());
            mAccountDescription.setText(mAccount.getDescription());
            mCurrencySelector.setSelection(adapter.getPosition(mAccount.getCurrency().getCode()));
            mColorSelector.setSelection(colorAdapter.getPosition(String.format("#%06X", (0xFFFFFF & mAccount.getColor()))));
            mInitialAmount.setText(mAccount.getAmount().toPlainString());
        } else {
            builder.setTitle(R.string.create_new_account).setView(dialog);
            mCurrencySelector.setSelection(adapter.getPosition("RUB"));
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
                if(mAccount != null)  { // modifying existing account
                    fillAccountFields();
                    final int result = ((WalletBaseActivity) getActivity()).getEntityDAO().updateAccount(mAccount);
                    if(result > 0)
                        dismiss();
                    else
                        Toast.makeText(getActivity(), R.string.account_not_found, Toast.LENGTH_SHORT).show();
                } else {  // creating new account
                    mAccount = new Account();
                    fillAccountFields();
                    final long insertRes = ((WalletBaseActivity) getActivity()).getEntityDAO().addAccount(mAccount);
                    if(insertRes != -1)
                        dismiss();
                    else
                        Toast.makeText(getActivity(), R.string.account_already_exist, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void fillAccountFields() {
        mAccount.setName(mAccountName.getText().toString());
        mAccount.setDescription(mAccountDescription.getText().toString());
        mAccount.setCurrency((Currency) mCurrencySelector.getSelectedItem());
        mAccount.setAmount(Utils.getValue(mInitialAmount.getText().toString(), BigDecimal.ZERO));
        mAccount.setColor(Color.parseColor((String) mColorSelector.getSelectedItem()));
    }

    public class ColorSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
        public ColorSpinnerAdapter(String[] objects) {
            super(getActivity(), R.layout.color_list_item, objects);
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

    public class CurrencyAdapter extends BaseAdapter {
        private final Context mContext;
        private final Cursor mCursor;

        public CurrencyAdapter() {
            mContext = getActivity();
            mCursor = getWalletActivity().getEntityDAO().getCurrencyCursor();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            mCursor.moveToPosition(position);

            if (convertView == null)
                view = View.inflate(mContext, android.R.layout.simple_spinner_item, null);
            else
                view = convertView;

            final TextView code = (TextView) view.findViewById(android.R.id.text1);
            code.setText(mCursor.getString(DatabaseDAO.CurrenciesFields.CODE.ordinal()));

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view;
            mCursor.moveToPosition(position);

            if (convertView == null)
                view = View.inflate(mContext, R.layout.currency_list_item, null);
            else
                view = convertView;

            final TextView code = (TextView) view.findViewById(R.id.curr_caption_text);
            code.setText(mCursor.getString(DatabaseDAO.CurrenciesFields.CODE.ordinal()));
            final TextView desc = (TextView) view.findViewById(R.id.curr_description_text);
            desc.setText(mCursor.getString(DatabaseDAO.CurrenciesFields.DESCRIPTION.ordinal()));
            final TextView usedIn = (TextView) view.findViewById(R.id.curr_usedin_text);
            usedIn.setText(mCursor.getString(DatabaseDAO.CurrenciesFields.USED_IN.ordinal()));

            return view;
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public Currency getItem(int position) {
            mCursor.moveToPosition(position);
            return getWalletActivity().getEntityDAO().getCurrency(mCursor.getString(DatabaseDAO.CurrenciesFields.CODE.ordinal()));
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getPosition(String code) {
            if(mCursor.moveToFirst())
                do {
                    if(mCursor.getString(DatabaseDAO.CurrenciesFields.CODE.ordinal()).equals(code))
                        return mCursor.getPosition();
                } while(mCursor.moveToNext());

            return -1;
        }
    }
}
