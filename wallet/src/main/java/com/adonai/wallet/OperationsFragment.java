package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
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
import com.adonai.wallet.entities.Operation;
import com.adonai.wallet.adapters.UUIDCursorAdapter;
import com.adonai.wallet.view.OperationView;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.adonai.wallet.WalletBaseFilterFragment.FilterType;

/**
 * Fragment that is responsible for showing operations list
 * and their context actions
 *
 * @author adonai
 */
public class OperationsFragment extends WalletBaseListFragment {

    private BroadcastReceiver mBackPressListener = new DbBroadcastReceiver();
    private final EntityDeleteListener mOperationDeleter = new EntityDeleteListener(R.string.really_delete_operation);
    private boolean isListFiltered = false;

    private RetrieveContentsCallback mContentRetrieveCallback = new RetrieveContentsCallback();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.operations_flow, container, false);
        assert rootView != null;

        mEntityList = (ListView) rootView.findViewById(R.id.operations_list);
        getLoaderManager().initLoader(Utils.OPERATIONS_LOADER, Bundle.EMPTY, mContentRetrieveCallback);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.operations_flow, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.operation_filter).setVisible(!isListFiltered);
        menu.findItem(R.id.operation_reset_filter).setVisible(isListFiltered);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        OperationsAdapter adapter = (OperationsAdapter) mEntityList.getAdapter();
        
        switch (item.getItemId()) {
            case R.id.action_add_operation:
                final OperationDialogFragment opCreate = new OperationDialogFragment();
                opCreate.show(getFragmentManager(), "opCreate");
                return true;
            case R.id.operation_filter:
                // form filtering map
                Map<String, Pair<FilterType, String>> allowedToFilter = new HashMap<>(3);
                allowedToFilter.put(getString(R.string.description), Pair.create(FilterType.TEXT, "description"));
                allowedToFilter.put(getString(R.string.amount), Pair.create(FilterType.AMOUNT, "amount"));
                allowedToFilter.put(getString(R.string.category), Pair.create(FilterType.FOREIGN_ID, "category"));
                allowedToFilter.put(getString(R.string.date), Pair.create(FilterType.DATE, "time"));
                final WalletBaseFilterFragment<Operation> opFilter = WalletBaseFilterFragment.newInstance(Operation.class, allowedToFilter);
                opFilter.setFilterCursorListener(adapter);
                opFilter.show(getFragmentManager(), "opFilter");
                return true;
            case R.id.operation_reset_filter:
                adapter.resetFilter();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class OperationsAdapter extends UUIDCursorAdapter<Operation> implements WalletBaseFilterFragment.FilterCursorListener<Operation> {
        public OperationsAdapter() {
            super(getActivity(), Operation.class,
                    DbProvider.getHelper().getEntityDao(Operation.class).queryBuilder().orderBy("time", false));
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public View getView(int position, View convertView, ViewGroup parent) {
            final OperationView view;

            if (convertView == null)
                view = new OperationView(mContext);
            else
                view = (OperationView) convertView;

            try {
                ((AndroidDatabaseResults) mCursor.getRawResults()).moveAbsolute(position);
                Operation op = mCursor.current();
                view.setOperation(op);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return view;
        }

        @Override
        public void onFilterCompleted(QueryBuilder<Operation, UUID> qBuilder) {
            setQuery(qBuilder);
            isListFiltered = true;
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBackPressListener, new IntentFilter(Utils.BACK_PRESSED));
            getActivity().invalidateOptionsMenu();
        }

        @Override
        public void setQuery(QueryBuilder<Operation, UUID> qBuilder) {
            try {
                qBuilder.orderBy("time", false);
                mQuery = qBuilder.prepare();
                mCursor = mDao.iterator(mQuery);
                notifyDataSetChanged();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void resetFilter() {
            setQuery((PreparedQuery<Operation>) null);
            isListFiltered = false;
            getActivity().invalidateOptionsMenu();
        }
    }

    private class OperationLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setItems(R.array.entity_choice_operation, new OperationChoice(position)).setTitle(R.string.select_action).create().show();
            return true;
        }
    }

    private class OperationChoice extends EntityChoice {

        public OperationChoice(int mItemPosition) {
            super(mItemPosition);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            try {
                OperationsAdapter adapter = (OperationsAdapter) mEntityList.getAdapter();
                final UUID opID = adapter.getItemUUID(mItemPosition);
                final Operation operation = DbProvider.getHelper().getOperationDao().queryForId(opID);
                switch (which) {
                    case 0: // modify
                        OperationDialogFragment.forOperation(operation.getId().toString()).show(getFragmentManager(), "opModify");
                        break;
                    case 1: // delete
                        mOperationDeleter.handleRemoveAttempt(operation);
                        break;
                    case 2: // cancel operation
                        Operation.revertOperation(operation);
                        break;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBackPressListener);
        } else if(isListFiltered) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBackPressListener, new IntentFilter(Utils.BACK_PRESSED));
        }
    }

    /**
     * Handler to receive notifications for back key pressed
     */
    private class DbBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            OperationsAdapter adapter = (OperationsAdapter) mEntityList.getAdapter();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBackPressListener);
            adapter.resetFilter();
        }
    }

    private class RetrieveContentsCallback implements LoaderManager.LoaderCallbacks<OperationsAdapter> {
        @Override
        public Loader<OperationsAdapter> onCreateLoader(int id, @NonNull final Bundle args) {
            AbstractAsyncLoader<OperationsAdapter> toRegister = new AbstractAsyncLoader<OperationsAdapter>(getActivity()) {
                @Nullable
                @Override
                public OperationsAdapter loadInBackground() {
                    if(!isStarted()) // task was cancelled
                        return null;

                    // check the DB for accounts
                    return new OperationsAdapter();
                }

                @Override
                protected void onForceLoad() {
                    if(mData != null) { // close old adapter before loading new one
                        mData.closeCursor();
                    }
                    super.onForceLoad();
                }
            };
            EntityDao<Operation> accDao = DbProvider.getHelper().getDao(Operation.class);
            accDao.registerObserver(toRegister);
            return toRegister;
        }

        @Override
        public void onLoadFinished(Loader<OperationsAdapter> loader, OperationsAdapter data) {
            mEntityList.setAdapter(data);
            mEntityList.setOnItemLongClickListener(new OperationLongClickListener());
        }

        @Override
        public void onLoaderReset(Loader<OperationsAdapter> loader) {
        }

    }
}
