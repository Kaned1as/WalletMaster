package com.adonai.wallet;

import android.app.ActionBar;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.UUIDCursorAdapter;

/**
 * Fragment that is responsible for showing categories list
 * and their context actions
 *
 * @author adonai
 */
public class CategoriesFragment extends WalletBaseListFragment {

    private SpinnerAdapter mCategoryTypeAdapter;
    private CategoriesAdapter mCategoriesAdapter;
    private ActionBar.OnNavigationListener mNavListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.categories_flow, container, false);
        assert rootView != null;

        final String[] categoryTypes = new String[] {getString(R.string.outcome), getString(R.string.income), getString(R.string.transfer)};
        mCategoryTypeAdapter = new ArrayAdapter<>(getActivity(), R.layout.tall_list_item, categoryTypes);
        mNavListener = new CategoryNavigator();

        mEntityList = (ListView) rootView.findViewById(R.id.categories_list);
        mEntityList.setOnItemLongClickListener(new CategoryEditListener());

        mCategoriesAdapter = new CategoriesAdapter(Category.EXPENSE);
        getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.EntityType.CATEGORIES.toString(), mCategoriesAdapter);
        getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mCategoriesAdapter); // foreign keys
        mEntityList.setAdapter(mCategoriesAdapter);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(DatabaseDAO.EntityType.CATEGORIES.toString(), mCategoriesAdapter);
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(DatabaseDAO.EntityType.ACCOUNTS.toString(), mCategoriesAdapter);
        mCategoriesAdapter.changeCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.categories_flow, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final ActionBar actBar = getActivity().getActionBar();
        actBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actBar.setListNavigationCallbacks(mCategoryTypeAdapter, mNavListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_category:
                final CategoryDialogFragment fragment = CategoryDialogFragment.newInstance(mCategoriesAdapter.getCategoryType());
                fragment.show(getFragmentManager(), "categoryCreate");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CategoryNavigator implements ActionBar.OnNavigationListener {

        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            mCategoriesAdapter.setCategoryType(itemPosition);
            return true;
        }
    }

    private class CategoriesAdapter extends UUIDCursorAdapter implements SpinnerAdapter, DatabaseDAO.DatabaseListener {
        private int mCategoryType;

        public CategoriesAdapter(int categoryType) {
            super(getActivity(), getWalletActivity().getEntityDAO().getCategoryCursor(categoryType));
            mCategoryType = categoryType;
        }

        @Override
        public void handleUpdate() {
            getWalletActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeCursor(getWalletActivity().getEntityDAO().getCategoryCursor(mCategoryType));
                }
            });

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return newView(position, convertView, parent, R.layout.category_list_item);
        }

        public View newView(int position, View convertView, ViewGroup parent, int resId) {
            final View view;
            mCursor.moveToPosition(position);

            if (convertView == null) {
                final LayoutInflater inflater = LayoutInflater.from(mContext);
                view = inflater.inflate(resId, parent, false);
            } else
                view = convertView;

            final TextView name = (TextView) view.findViewById(android.R.id.text1);
            name.setText(mCursor.getString(1));

            return view;
        }

        public void setCategoryType(int type) {
            mCategoryType = type;
            handleUpdate();
        }

        public int getCategoryType() {
            return mCategoryType;
        }
    }

    private class CategoryEditListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setItems(R.array.entity_choice_common, new CategoryChoice(position)).setTitle(R.string.select_action).create().show();
            return true;
        }
    }

    private class CategoryChoice extends EntityChoice {

        public CategoryChoice(int mItemPosition) {
            super(mItemPosition);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0: // modify
                    final String categoryID = mCategoriesAdapter.getItemUUID(mItemPosition);
                    final CategoryDialogFragment fragment = CategoryDialogFragment.forCategory(categoryID);
                    fragment.show(getFragmentManager(), "categoryCreate");
                    break;
                case 1: // delete
                    final DatabaseDAO db = getWalletActivity().getEntityDAO();
                    final String categoryId = mCategoriesAdapter.getItemUUID(mItemPosition);
                    db.makeAction(DatabaseDAO.ActionType.DELETE, Category.getFromDB(db, categoryId));
                    break;
            }
        }
    }
}
