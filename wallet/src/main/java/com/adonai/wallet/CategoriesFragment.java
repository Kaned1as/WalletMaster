package com.adonai.wallet;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.UUIDCursorAdapter;

import org.thirdparty.contrib.SwipeDismissListViewTouchListener;

/**
 * Fragment that is responsible for showing categories list
 * and their context actions
 *
 * @author adonai
 */
public class CategoriesFragment extends WalletBaseFragment {

    private SpinnerAdapter mCategoryTypeAdapter;
    private CategoriesAdapter mCategoriesAdapter;
    private ActionBar.OnNavigationListener mNavListener;

    private ListView mCategoryList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.categories_flow, container, false);
        assert rootView != null;

        final String[] categoryTypes = new String[] {getString(R.string.expense), getString(R.string.income), getString(R.string.transfer)};
        mCategoryTypeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, categoryTypes);
        mNavListener = new CategoryNavigator();

        mCategoryList = (ListView) rootView.findViewById(R.id.categories_list);
        final View footer = LayoutInflater.from(getActivity()).inflate(R.layout.listview_add_footer, mCategoryList, false);
        footer.setOnClickListener(new CategoryAddListener());
        mCategoryList.addFooterView(footer);
        mCategoryList.setOnItemLongClickListener(new CategoryEditListener());

        mCategoriesAdapter = new CategoriesAdapter(Category.EXPENSE);
        mCategoryList.setAdapter(mCategoriesAdapter);

        final SwipeDismissListViewTouchListener listener = new SwipeDismissListViewTouchListener(mCategoryList, new CategoryDeleteListener());


        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final ActionBar actBar = getActivity().getActionBar();
        actBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actBar.setListNavigationCallbacks(mCategoryTypeAdapter, mNavListener);
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
            getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.EntityType.CATEGORIES.toString(), this);
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
            return newView(position, convertView, parent, R.layout.category_list_item_large);
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

    private class CategoryAddListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            final CategoryDialogFragment fragment = CategoryDialogFragment.newInstance(mCategoriesAdapter.getCategoryType());
            fragment.show(getFragmentManager(), "categoryCreate");
        }
    }

    private class CategoryDeleteListener implements SwipeDismissListViewTouchListener.DismissCallbacks {

        @Override
        public boolean canDismiss(int position) {
            return true;
        }

        @Override
        public void onDismiss(ListView listView, int[] reverseSortedPositions) {
            final DatabaseDAO db = getWalletActivity().getEntityDAO();
            for(int catPos : reverseSortedPositions) {
                final String categoryId = mCategoriesAdapter.getItemUUID(catPos);
                Category.getFromDB(db, categoryId).delete(db);
            }
        }
    }

    private class CategoryEditListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final String categoryID = mCategoriesAdapter.getItemUUID(position);
            final CategoryDialogFragment fragment = CategoryDialogFragment.forCategory(categoryID);
            fragment.show(getFragmentManager(), "categoryCreate");
            return true;
        }
    }
}
