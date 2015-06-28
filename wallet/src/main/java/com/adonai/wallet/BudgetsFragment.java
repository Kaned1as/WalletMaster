package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.adonai.wallet.database.AbstractAsyncLoader;
import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
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

    private final EntityDeleteListener mBudgetDeleter = new EntityDeleteListener(R.string.really_delete_budget);

    private RetrieveContentsCallback mContentRetrieveCallback = new RetrieveContentsCallback();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.budgets_flow, container, false);
        assert rootView != null;

        mEntityList = (ListView) rootView.findViewById(R.id.budgets_list);
        getLoaderManager().initLoader(Utils.BUDGETS_LOADER, Bundle.EMPTY, mContentRetrieveCallback);

        return rootView;
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
            BudgetsAdapter adapter = (BudgetsAdapter) mEntityList.getAdapter();
            UUID budgetID = adapter.getItemUUID(mItemPosition);
            Budget budget = DbProvider.getHelper().getBudgetDao().queryForId(budgetID);
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

    private class RetrieveContentsCallback implements LoaderManager.LoaderCallbacks<BudgetsAdapter> {
        @Override
        public Loader<BudgetsAdapter> onCreateLoader(int id, @NonNull final Bundle args) {
            AbstractAsyncLoader<BudgetsAdapter> toRegister = new AbstractAsyncLoader<BudgetsAdapter>(getActivity()) {
                @Nullable
                @Override
                public BudgetsAdapter loadInBackground() {
                    if(!isStarted()) // task was cancelled
                        return null;

                    // check the DB for accounts
                    return new BudgetsAdapter();
                }

                @Override
                protected void onForceLoad() {
                    if(mData != null) { // close old adapter before loading new one
                        mData.closeCursor();
                    }
                    super.onForceLoad();
                }
            };
            EntityDao<Budget> bgDao = DbProvider.getHelper().getDao(Budget.class);
            EntityDao<BudgetItem> bgiDao = DbProvider.getHelper().getDao(BudgetItem.class);
            bgDao.registerObserver(toRegister);
            bgiDao.registerObserver(toRegister); // budgetItem is dependant, so update views on its change too
            return toRegister;
        }

        @Override
        public void onLoadFinished(Loader<BudgetsAdapter> loader, BudgetsAdapter data) {
            mEntityList.setAdapter(data);
            mEntityList.setOnItemLongClickListener(new BudgetLongClickListener());
        }

        @Override
        public void onLoaderReset(Loader<BudgetsAdapter> loader) {
        }

    }
}
