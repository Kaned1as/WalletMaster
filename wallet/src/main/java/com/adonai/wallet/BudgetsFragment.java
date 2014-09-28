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
import android.widget.Toast;

import com.adonai.wallet.database.DatabaseFactory;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.adonai.wallet.view.BudgetView;

import java.sql.SQLException;
import java.util.ArrayList;
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
        final ArrayList<BudgetView> shadow = (ArrayList<BudgetView>) mBudgetsAdapter.mExpandedBudgets.clone();
        for(BudgetView view : shadow)
            view.collapse(); // close all child cursors

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


    public class BudgetsAdapter extends UUIDCursorAdapter<Budget> implements DatabaseDAO.DatabaseListener {
        private final ArrayList<BudgetView> mExpandedBudgets = new ArrayList<>();

        public BudgetsAdapter() {
            super(getActivity(), DatabaseFactory.getHelper().getBudgetDao());
        }

        @Override
        public void handleUpdate() {
            notifyDataSetChanged();
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public View getView(int position, View convertView, ViewGroup parent) {
            final BudgetView view;

            if (convertView == null)
                view = new BudgetView(mContext, this);
            else
                view = (BudgetView) convertView;

            try {
                mCursor.first();
                final Budget forView = mCursor.moveRelative(position);
                view.setBudget(forView);
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.database_error) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

            return view;
        }

        public void addExpandedView(BudgetView view) {
            mExpandedBudgets.add(view);
        }

        public void removeExpandedView(BudgetView view) {
            mExpandedBudgets.remove(view);
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
            try {
                final UUID budgetID = mBudgetsAdapter.getItemUUID(mItemPosition);
                final Budget budget = DatabaseFactory.getHelper().getBudgetDao().queryForId(budgetID);
                switch (which) {
                    case 0: // modify
                        BudgetDialogFragment.forBudget(budget.getId().toString()).show(getFragmentManager(), "budgetModify");
                        break;
                    case 1: // delete
                        mBudgetDeleter.handleRemoveAttempt(budget);
                        break;
                }
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.database_error) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


}
