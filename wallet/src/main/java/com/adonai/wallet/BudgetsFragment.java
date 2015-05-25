package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.Operation;
import com.adonai.wallet.adapters.UUIDCursorAdapter;
import com.adonai.wallet.view.BudgetView;

import java.sql.SQLException;
import java.util.UUID;

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
        final View rootView = inflater.inflate(R.layout.budgets_flow, container, false);
        assert rootView != null;

        mEntityList = (ListView) rootView.findViewById(R.id.budgets_list);


        mEntityList.setAdapter(mBudgetsAdapter);
        mEntityList.setOnItemLongClickListener(new BudgetLongClickListener());

        return rootView;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onDestroyView() {
        super.onDestroyView();
        mBudgetsAdapter.closeCursor();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.budgets_flow, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_budget:
                final BudgetDialogFragment budgetCreate = new BudgetDialogFragment();
                budgetCreate.show(getFragmentManager(), "budgetCreate");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public class BudgetsAdapter extends UUIDCursorAdapter<Budget> {
        public BudgetsAdapter() {
            super(getActivity(), Budget.class);
            DbProvider.getHelper().getEntityDao(Operation.class).registerObserver(this);
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public View getView(int position, View convertView, ViewGroup parent) {
            final BudgetView view;

            if (convertView == null)
                view = new BudgetView(mContext);
            else
                view = (BudgetView) convertView;

            try {
                mCursor.first();
                final Budget forView = mCursor.moveRelative(position);
                view.setBudget(forView);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return view;
        }
    }


    private class BudgetLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setItems(R.array.entity_choice_common, new BudgetChoice(position)).setTitle(R.string.select_action).create().show();
            return true;
        }
    }

    private class BudgetChoice extends EntityChoice {

        public BudgetChoice(int mItemPosition) {
            super(mItemPosition);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final UUID budgetID = mBudgetsAdapter.getItemUUID(mItemPosition);
            final Budget budget = DbProvider.getHelper().getBudgetDao().queryForId(budgetID);
            switch (which) {
                case 0: // modify
                    BudgetDialogFragment.forBudget(budget.getId().toString()).show(getFragmentManager(), "budgetModify");
                    break;
                case 1: // delete
                    mBudgetDeleter.handleRemoveAttempt(budget);
                    break;
            }
        }
    }


}
