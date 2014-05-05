package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.daniel.lupianez.casares.PopoverView;

import java.util.Arrays;
import java.util.UUID;

import static com.adonai.wallet.Utils.convertDpToPixel;

/**
 * A placeholder fragment containing a simple view.
 */
public class AccountsFragment extends WalletBaseFragment {

    private ListView mAccountList;
    private AccountsAdapter mAccountsAdapter;
    private TextView budgetSum;

    private boolean mLoadOnStart = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.accounts_flow, container, false);
        assert rootView != null;

        mAccountList = (ListView) rootView.findViewById(R.id.account_list);
        budgetSum = (TextView) rootView.findViewById(R.id.account_sum);

        mAccountsAdapter = new AccountsAdapter();

        mAccountList.setAdapter(mAccountsAdapter);
        getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mAccountsAdapter);
        mAccountList.setOnItemLongClickListener(new AccountLongClickListener());
        mAccountList.setOnItemClickListener(new AccountClickListener());

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
            super(getActivity(), getWalletActivity().getEntityDAO().getAccountCursor());
        }

        @Override
        public void handleUpdate() {
            getWalletActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeCursor(getWalletActivity().getEntityDAO().getAccountCursor());
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
            view.setBackgroundDrawable(mDrawable);

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
            final View newView = LayoutInflater.from(getActivity()).inflate(R.layout.account_list_item_menu, null, false);
            final PopoverView popover = new PopoverView(getActivity(), newView);
            popover.setContentSizeForViewInPopover(new Point((int) convertDpToPixel(100, getActivity()), (int) convertDpToPixel(50, getActivity())));

            final ImageButton delete = (ImageButton) newView.findViewById(R.id.delete_button);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.confirm_action)
                            .setMessage(R.string.really_delete_account)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final UUID accountID = mAccountsAdapter.getItemUUID(position);
                                    getWalletActivity().getEntityDAO().makeAction(DatabaseDAO.ActionType.DELETE, Account.getFromDB(getWalletActivity().getEntityDAO(), accountID.toString()));
                                }
                            }).create().show();

                    popover.dismissPopover(true);
                }
            });

            final ImageButton edit = (ImageButton) newView.findViewById(R.id.edit_button);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final UUID accountID = mAccountsAdapter.getItemUUID(position);
                    final Account managed = Account.getFromDB(getWalletActivity().getEntityDAO(), accountID.toString());
                    new AccountDialogFragment(managed).show(getFragmentManager(), "accModify");
                    popover.dismissPopover(true);
                }
            });

            popover.showPopoverFromRectInViewGroup((ViewGroup) parent.getRootView(), PopoverView.getFrameForView(view), PopoverView.PopoverArrowDirectionAny, true);
            return true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mAccountsAdapter);
        mAccountsAdapter.changeCursor(null); // close opened cursor
    }

    private class AccountClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final UUID accountID = mAccountsAdapter.getItemUUID(position);
            final Account managed = Account.getFromDB(getWalletActivity().getEntityDAO(), accountID.toString());
            new OperationDialogFragment(managed).show(getFragmentManager(), "opModify");
        }
    }
}
