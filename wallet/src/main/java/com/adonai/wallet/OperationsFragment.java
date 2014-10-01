package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Operation;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.adonai.wallet.view.OperationView;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Fragment that is responsible for showing operations list
 * and their context actions
 * Uses async operation load for better interactivity
 *
 * @author adonai
 */
public class OperationsFragment extends WalletBaseListFragment {

    private MenuItem mSearchItem;

    private OperationsAdapter mOpAdapter;
    private final EntityDeleteListener mOperationDeleter = new EntityDeleteListener(R.string.really_delete_operation);
    private final QuickSearchQueryHandler mQuickSearchHandler = new QuickSearchQueryHandler();
    private boolean isListFiltered = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mOpAdapter = new OperationsAdapter();

        final View rootView = inflater.inflate(R.layout.operations_flow, container, false);
        assert rootView != null;

        mEntityList = (ListView) rootView.findViewById(R.id.operations_list);

        mEntityList.setAdapter(mOpAdapter);
        mEntityList.setOnItemLongClickListener(new OperationLongClickListener());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mOpAdapter.closeCursor(); // close opened cursor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.operations_flow, menu);

        final SearchManager manager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchItem = menu.findItem(R.id.operation_quick_filter);
        final SearchView search = (SearchView) mSearchItem.getActionView();
        search.setQueryHint(getString(R.string.quick_filter));
        search.setSearchableInfo(manager.getSearchableInfo(getActivity().getComponentName()));
        search.setOnQueryTextListener(mQuickSearchHandler);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.operation_filter).setVisible(!isListFiltered);
        menu.findItem(R.id.operation_quick_filter).setVisible(!isListFiltered);
        menu.findItem(R.id.operation_reset_filter).setVisible(isListFiltered);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_operation:
                final OperationDialogFragment opCreate = new OperationDialogFragment();
                opCreate.show(getFragmentManager(), "opCreate");
                break;
            case R.id.operation_filter:
                // form filtering map
                /*
                final Map<String, Pair<FilterType, Object>> allowedToFilter = new HashMap<>(3);
                allowedToFilter.put(getString(R.string.description), new Pair<FilterType, Object>(FilterType.TEXT, OperationsFields.DESCRIPTION.toString()));
                allowedToFilter.put(getString(R.string.amount), new Pair<FilterType, Object>(FilterType.AMOUNT, OperationsFields.AMOUNT.toString()));
                final Cursor foreignCursor = DatabaseDAO.getInstance().getForeignNameCursor(OPERATIONS, OperationsFields.CATEGORY.toString(), CATEGORIES, CategoriesFields.NAME.toString());
                allowedToFilter.put(getString(R.string.category), new Pair<FilterType, Object>(FilterType.FOREIGN_ID, foreignCursor));
                allowedToFilter.put(getString(R.string.date), new Pair<FilterType, Object>(FilterType.DATE, OperationsFields.TIME.toString()));
                final WalletBaseFilterFragment opFilter = WalletBaseFilterFragment.newInstance(OPERATIONS.toString(), allowedToFilter);
                opFilter.setFilterCursorListener(mOpAdapter);
                opFilter.show(getFragmentManager(), "opFilter");
                */
            case R.id.operation_reset_filter:
                mOpAdapter.resetFilter();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class OperationsAdapter extends UUIDCursorAdapter<Operation> implements WalletBaseFilterFragment.FilterCursorListener {
        public OperationsAdapter() {
            super(getActivity(), DbProvider.getHelper().getEntityDao(Operation.class));
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
                mCursor.first();
                Operation op = mCursor.moveRelative(position);
                view.setOperation(op);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return view;
        }

        @Override
        public void OnFilterCompleted(Cursor cursor) {
            //changeCursor(cursor);
            isListFiltered = true;
            getActivity().invalidateOptionsMenu();
        }

        @Override
        public void resetFilter() {
            //changeCursor(DatabaseDAO.getInstance().getOperationsCursor());
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
                final UUID opID = mOpAdapter.getItemUUID(mItemPosition);
                final Operation operation = DbProvider.getHelper().getOperationDao().queryForId(opID);
                switch (which) {
                    case 0: // modify
                        new OperationDialogFragment(operation).show(getFragmentManager(), "opModify");
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

    private class QuickSearchQueryHandler implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if(!query.isEmpty()) {
                //mOpAdapter.changeCursor(DatabaseDAO.getInstance().getOperationsCursor(query));
                if (mSearchItem != null)
                    mSearchItem.collapseActionView(); // hide after submit
                isListFiltered = true;
                getActivity().invalidateOptionsMenu(); // update filter buttons visibility
            }

            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }
}
