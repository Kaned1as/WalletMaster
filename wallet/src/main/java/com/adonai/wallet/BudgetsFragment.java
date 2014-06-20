package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.adonai.wallet.view.BudgetView;

import static com.adonai.wallet.DatabaseDAO.EntityType.BUDGETS;
import static com.adonai.wallet.DatabaseDAO.EntityType.OPERATIONS;

/**
 * Fragment that is responsible for showing budget list
 * and its context actions
 *
 * @author adonai
 */
public class BudgetsFragment extends WalletBaseListFragment {

    private BudgetsAdapter mBudgetsAdapter;
    private final EntityDeleteListener mBudgetDeleter = new EntityDeleteListener(R.string.really_delete_budget);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        mBudgetsAdapter = new BudgetsAdapter();
        getWalletActivity().getEntityDAO().registerDatabaseListener(BUDGETS.toString(), mBudgetsAdapter);
        getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mBudgetsAdapter); // due to foreign key null setting

        final View rootView = inflater.inflate(R.layout.budgets_flow, container, false);
        assert rootView != null;

        mEntityList = (ListView) rootView.findViewById(R.id.budgets_list);


        mEntityList.setAdapter(mBudgetsAdapter);
        mEntityList.setOnItemLongClickListener(new BudgetLongClickListener());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(BUDGETS.toString(), mBudgetsAdapter);
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mBudgetsAdapter); // due to foreign key null setting
        mBudgetsAdapter.changeCursor(null); // close opened cursor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.budgets_flow, menu);
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_budget:
                final OperationDialogFragment opCreate = new OperationDialogFragment();
                opCreate.show(getFragmentManager(), "opCreate");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
*/

    private class BudgetsAdapter extends UUIDCursorAdapter implements DatabaseDAO.DatabaseListener {
        public BudgetsAdapter() {
            super(getActivity(), getWalletActivity().getEntityDAO().getBudgetsCursor());
        }

        @Override
        public void handleUpdate() {
            getWalletActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeCursor(getWalletActivity().getEntityDAO().getBudgetsCursor());
                }
            });
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public View getView(int position, View convertView, ViewGroup parent) {
            final BudgetView view;
            final DatabaseDAO db = getWalletActivity().getEntityDAO();
            mCursor.moveToPosition(position);

            if (convertView == null)
                view = new BudgetView(mContext);
            else
                view = (BudgetView) convertView;

            view.setBudget(Budget.getFromDB(db, mCursor.getString(DatabaseDAO.BudgetFields._id.ordinal())));

            return view;
        }
    }


    private class BudgetLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setItems(R.array.entity_choice_operation, new BudgetChoice(position)).setTitle(R.string.select_action).create().show();
            return true;
        }
    }



    private class BudgetChoice extends EntityChoice {

        public BudgetChoice(int mItemPosition) {
            super(mItemPosition);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final DatabaseDAO db = getWalletActivity().getEntityDAO();
            final String accID = mBudgetsAdapter.getItemUUID(mItemPosition);
            final Account acc = Account.getFromDB(db, accID);
            switch (which) {
                case 0: // modify
                    AccountDialogFragment.forAccount(acc.getId()).show(getFragmentManager(), "accModify");
                    break;
                case 1: // delete
                    mBudgetDeleter.handleRemoveAttempt(acc);
                    break;
            }
        }
    }


}
