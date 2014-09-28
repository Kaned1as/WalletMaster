package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Currency;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.QueryBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Dialog fragment showing window for account modifying/adding
 *
 * @author adonai
 */
public class AccountDialogFragment extends WalletBaseDialogFragment implements DialogInterface.OnClickListener {
    private final static String ACCOUNT_REFERENCE = "account.reference";

    private EditText mAccountName;
    private EditText mAccountDescription;
    private Spinner mCurrencySelector;
    private Spinner mColorSelector;
    private EditText mInitialAmount;

    private CurrencyAdapter mCurrAdapter;

    public static AccountDialogFragment forAccount(String accountId) {
        final AccountDialogFragment fragment = new AccountDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ACCOUNT_REFERENCE, accountId);
        fragment.setArguments(args);
        return fragment;
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

        mCurrAdapter = new CurrencyAdapter();
        mCurrencySelector.setAdapter(mCurrAdapter);
        final ColorSpinnerAdapter mColorAdapter = new ColorSpinnerAdapter(getResources().getStringArray(R.array.colors));
        mColorSelector.setAdapter(mColorAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(getArguments() != null && getArguments().containsKey(ACCOUNT_REFERENCE)) {
            Account modAcc = DbProvider.getHelper().getAccountDao().queryForId(UUID.fromString(getArguments().getString(ACCOUNT_REFERENCE)));

            builder.setPositiveButton(R.string.confirm, this);
            builder.setTitle(R.string.edit_account).setView(dialog);

            mAccountName.setText(modAcc.getName());
            mAccountDescription.setText(modAcc.getDescription());
            mCurrencySelector.setSelection(mCurrAdapter.getPosition(modAcc.getCurrency().getCode()));
            mColorSelector.setSelection(mColorAdapter.getPosition(String.format("#%06X", (0xFFFFFF & modAcc.getColor()))));
            mInitialAmount.setText(modAcc.getAmount().toPlainString());
        } else {
            builder.setPositiveButton(R.string.create, this);
            builder.setTitle(R.string.create_new_account).setView(dialog);
            mCurrencySelector.setSelection(mCurrAdapter.getPosition("RUB"));
        }


        return builder.create();
    }

    private void fillAccountFieldsFromGUI(Account acc) {
        acc.setName(mAccountName.getText().toString());
        acc.setDescription(mAccountDescription.getText().toString());
        acc.setCurrency((Currency) mCurrencySelector.getSelectedItem());
        acc.setAmount(Utils.getValue(mInitialAmount.getText().toString(), BigDecimal.ZERO));
        acc.setColor(Color.parseColor((String) mColorSelector.getSelectedItem()));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(mAccountName.getText() == null || mAccountName.getText().toString().equals("")) {
            Toast.makeText(getActivity(), R.string.account_name_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        Account tmp;
        if(getArguments() != null && getArguments().containsKey(ACCOUNT_REFERENCE))  { // modifying existing account
            tmp = DbProvider.getHelper().getAccountDao().queryForId(UUID.fromString(getArguments().getString(ACCOUNT_REFERENCE)));
        } else // creating new
            tmp = new Account();
        fillAccountFieldsFromGUI(tmp);
        DbProvider.getHelper().getAccountDao().createOrUpdate(tmp);
        dismiss();
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
                rowView = inflater.inflate(R.layout.color_list_item, parent, false);
                rowView.findViewById(R.id.color_view).setBackgroundColor(Color.parseColor(getItem(position)));
            } else
                rowView.findViewById(R.id.color_view).setBackgroundColor(Color.parseColor(getItem(position)));
            return rowView;
        }
    }

    public class CurrencyAdapter extends BaseAdapter {
        private final Context mContext;
        private final QueryBuilder<Currency, String> mQuery;
        private final CloseableIterator<Currency> mCursor;

        public CurrencyAdapter() {
            try {
                mContext = getActivity();
                mQuery = DbProvider.getHelper().getCurrencyDao().queryBuilder();
                mCursor = mQuery.iterator();
            } catch (SQLException e) { // should not happen
                throw new RuntimeException(e);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            final LayoutInflater inflater = LayoutInflater.from(mContext);

            if (convertView == null)
                view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            else
                view = convertView;

            try {
                mCursor.first();
                Currency cur = mCursor.moveRelative(position);

                final TextView code = (TextView) view.findViewById(android.R.id.text1);
                code.setText(cur.getCode());
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.database_error) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            final View view;
            final LayoutInflater inflater = LayoutInflater.from(mContext);

            if (convertView == null)
                view = inflater.inflate(R.layout.currency_list_item, parent, false);
            else
                view = convertView;

            try {
                mCursor.first();
                Currency cur = mCursor.moveRelative(position);

                final TextView code = (TextView) view.findViewById(R.id.curr_caption_text);
                code.setText(cur.getCode());
                final TextView desc = (TextView) view.findViewById(R.id.curr_description_text);
                desc.setText(cur.getDescription());
                final TextView usedIn = (TextView) view.findViewById(R.id.curr_usedin_text);
                usedIn.setText(cur.getUsedIn());
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.database_error) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

            return view;
        }

        @Override
        public int getCount() {
            try {
                return (int) mQuery.countOf();
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.database_error) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return 0;
            }
        }

        @Override
        public Currency getItem(int position) {
            try {
                mCursor.first();
                return mCursor.moveRelative(position);
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.database_error) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getPosition(String code) {
            try {
                int pos = 0;
                Currency first = mCursor.first();
                if(first.getCode().equals(code))
                    return pos;

                while (mCursor.hasNext()) {
                    ++pos;
                    Currency entity = mCursor.next();
                    if(entity.getCode().equals(code))
                        return pos;
                }
            } catch (SQLException e) {
                return  -1;
            }
            return -1;
        }

        public void closeCursor() {
            mCursor.closeQuietly();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCurrAdapter.closeCursor();
    }
}
