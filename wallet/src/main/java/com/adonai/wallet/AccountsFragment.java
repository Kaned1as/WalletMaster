package com.adonai.wallet;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Point;
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
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.adonai.wallet.entities.Account;
import com.daniel.lupianez.casares.PopoverView;

import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class AccountsFragment extends WalletBaseFragment {

    ListView mAccountList;
    AccountsAdapter mAccountsAdapter;
    TextView budgetSum;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.accounts_flow, container, false);
        assert rootView != null;

        mAccountList = (ListView) rootView.findViewById(R.id.account_list);
        budgetSum = (TextView) rootView.findViewById(R.id.account_sum);

        mAccountsAdapter = new AccountsAdapter();
        getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.ACCOUNTS_TABLE_NAME, mAccountsAdapter);
        mAccountList.setAdapter(mAccountsAdapter);
        mAccountList.setOnItemLongClickListener(new AccountLongClickListener());

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

    private class AccountsAdapter extends CursorAdapter implements DatabaseDAO.DatabaseListener {
        public AccountsAdapter() {
            super(getActivity(), getWalletActivity().getEntityDAO().getAccountCursor(), false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.account_list_item, viewGroup, false);
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public void bindView(View view, Context context, Cursor cursor) {

            final int accColor = cursor.getInt(5);
            final float[] rounds = new float[8];
            Arrays.fill(rounds, Utils.convertDpToPixel(10f, context));
            final ShapeDrawable mDrawable = new ShapeDrawable(new RoundRectShape(rounds, null, null));
            mDrawable.getPaint().setShader(new LinearGradient(0, 0, context.getResources().getDisplayMetrics().widthPixels, 0,
               Color.argb(50, Color.red(accColor), Color.green(accColor), Color.blue(accColor)),
               Color.argb(0, Color.red(accColor), Color.green(accColor), Color.blue(accColor)), Shader.TileMode.CLAMP));
            view.setBackgroundDrawable(mDrawable);

            final TextView name = (TextView) view.findViewById(R.id.account_name_label);
            name.setText(cursor.getString(1));
            final TextView description = (TextView) view.findViewById(R.id.account_description_label);
            description.setText(cursor.getString(2));
            final TextView currency = (TextView) view.findViewById(R.id.account_currency_label);
            currency.setText(cursor.getString(3));
            final TextView amount = (TextView) view.findViewById(R.id.account_amount_label);
            amount.setText(cursor.getString(4));
        }

        @Override
        public void handleUpdate() {
            changeCursor(getWalletActivity().getEntityDAO().getAccountCursor());
        }
    }

    private class AccountLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {
            final View newView = LayoutInflater.from(getActivity()).inflate(R.layout.account_list_item_menu, null, false);
            final PopoverView popover = new PopoverView(getActivity(), newView);
            popover.setContentSizeForViewInPopover(new Point((int) Utils.convertDpToPixel(100, getActivity()), (int) Utils.convertDpToPixel(50, getActivity())));

            final ImageButton delete = (ImageButton) newView.findViewById(R.id.delete_button);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getWalletActivity().getEntityDAO().deleteAccount(id);
                    popover.dissmissPopover(true);
                }
            });

            final ImageButton edit = (ImageButton) newView.findViewById(R.id.edit_button);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Account managed = getWalletActivity().getEntityDAO().getAccount(id);
                    new AccountDialogFragment(managed).show(getFragmentManager(), "accModify");
                    popover.dissmissPopover(true);
                }
            });

            popover.showPopoverFromRectInViewGroup((ViewGroup) parent.getRootView(), PopoverView.getFrameForView(view), PopoverView.PopoverArrowDirectionUp, true);
            return true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(DatabaseDAO.ACCOUNTS_TABLE_NAME, mAccountsAdapter);
        mAccountsAdapter.changeCursor(null); // close opened cursor
    }
}
