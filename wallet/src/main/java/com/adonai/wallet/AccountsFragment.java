package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.UUIDCursorAdapter;

import java.util.Arrays;

import static com.adonai.wallet.Utils.convertDpToPixel;

/**
 * Fragment responsible for showing accounts list
 * and their context actions
 *
 * @author Adonai
 */
public class AccountsFragment extends WalletBaseListFragment {

    private AccountsAdapter mAccountsAdapter;
    private TextView budgetSum;
    private final EntityDeleteListener mAccountDeleter = new EntityDeleteListener(R.string.really_delete_account);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.accounts_flow, container, false);
        assert rootView != null;

        mEntityList = (ListView) rootView.findViewById(R.id.account_list);
        budgetSum = (TextView) rootView.findViewById(R.id.account_sum);

        mAccountsAdapter = new AccountsAdapter();
        DatabaseDAO.getInstance().registerDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mAccountsAdapter);

        mEntityList.setAdapter(mAccountsAdapter);
        mEntityList.setOnItemLongClickListener(new AccountLongClickListener());
        mEntityList.setOnItemClickListener(new AccountClickListener());

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.accounts_flow, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_account:
                final AccountDialogFragment accountCreate = new AccountDialogFragment();
                accountCreate.show(getFragmentManager(), "accCreate");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class AccountsAdapter extends UUIDCursorAdapter implements DatabaseDAO.DatabaseListener {
        public AccountsAdapter() {
            super(getActivity(), DatabaseDAO.getInstance().getAccountCursor());
        }

        @Override
        public void handleUpdate() {
            getWalletActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeCursor(DatabaseDAO.getInstance().getAccountCursor());
                }
            });

        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            mCursor.moveToPosition(position);

            if (convertView == null)
                view = inflater.inflate(R.layout.account_list_item, parent, false);
            else
                view = convertView;

            final int accColor = mCursor.getInt(5);
            final float[] rounds = new float[8];
            Arrays.fill(rounds, convertDpToPixel(10f, mContext));
            final ShapeDrawable mDrawable = new ShapeDrawable(new RoundRectShape(rounds, null, null));
            mDrawable.getPaint().setShader(new LinearGradient(0, 0, mContext.getResources().getDisplayMetrics().widthPixels, 0,
                    Color.argb(50, Color.red(accColor), Color.green(accColor), Color.blue(accColor)),
                    Color.argb(0, Color.red(accColor), Color.green(accColor), Color.blue(accColor)), Shader.TileMode.CLAMP));
            view.findViewById(R.id.main_content_layout).setBackgroundDrawable(mDrawable);

            final TextView name = (TextView) view.findViewById(R.id.account_name_label);
            name.setText(mCursor.getString(1));
            final TextView description = (TextView) view.findViewById(R.id.account_description_label);
            description.setText(mCursor.getString(2));
            final TextView currency = (TextView) view.findViewById(R.id.account_currency_label);
            currency.setText(mCursor.getString(3));
            final TextView amount = (TextView) view.findViewById(R.id.account_amount_label);
            amount.setText(mCursor.getString(4));

            return view;
        }
    }

    private class AccountLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setItems(R.array.entity_choice_common, new AccountChoice(position)).setTitle(R.string.select_action).create().show();
            return true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DatabaseDAO.getInstance().unregisterDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mAccountsAdapter);
        mAccountsAdapter.changeCursor(null); // close opened cursor
    }

    private class AccountClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String accountID = mAccountsAdapter.getItemUUID(position);
            final Account managed = Account.getFromDB(accountID);
            new OperationDialogFragment(managed).show(getFragmentManager(), "opModify");
        }
    }

    private class AccountChoice extends EntityChoice {

        public AccountChoice(int mItemPosition) {
            super(mItemPosition);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final String accID = mAccountsAdapter.getItemUUID(mItemPosition);
            final Account acc = Account.getFromDB(accID);
            switch (which) {
                case 0: // modify
                    AccountDialogFragment.forAccount(acc.getId()).show(getFragmentManager(), "accModify");
                    break;
                case 1: // delete
                    mAccountDeleter.handleRemoveAttempt(acc);
                    break;
            }
        }
    }
}
