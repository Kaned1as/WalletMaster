package com.adonai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.adapters.UUIDCursorAdapter;

import java.sql.SQLException;
import java.util.UUID;

import static com.adonai.wallet.entities.Category.CategoryType;

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

        mCategoriesAdapter = new CategoriesAdapter(getActivity(), R.layout.category_list_item, CategoryType.EXPENSE);
        mEntityList.setAdapter(mCategoriesAdapter);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCategoriesAdapter.closeCursor();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.categories_flow, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        final ActionBar actBar = getWalletActivity().getSupportActionBar();
        if(hidden) {
            actBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        } else {
            actBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actBar.setListNavigationCallbacks(mCategoryTypeAdapter, mNavListener);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_category:
                final CategoryDialogFragment fragment = CategoryDialogFragment.newInstance(mCategoriesAdapter.getCategoryType().ordinal());
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
            mCategoriesAdapter.setCategoryType(CategoryType.values()[itemPosition]);
            return true;
        }
    }

    public static class CategoriesAdapter extends UUIDCursorAdapter<Category> implements SpinnerAdapter {
        private final int mResourceId;
        private CategoryType mCategoryType;

        public CategoriesAdapter(Activity context, int resourceId, CategoryType categoryType) {
            super(context, Category.class);
            try {
                mResourceId = resourceId;
                mCategoryType = categoryType;
                setQuery(DbProvider.getHelper().getCategoryDao().queryBuilder()
                        .where().eq("type", mCategoryType).and().eq("deleted", false).prepare());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return newView(position, convertView, parent, mResourceId);
        }

        public View newView(int position, View convertView, ViewGroup parent, int resId) {
            final View view;

            if (convertView == null) {
                final LayoutInflater inflater = LayoutInflater.from(mContext);
                view = inflater.inflate(resId, parent, false);
            } else
                view = convertView;

            try {
                mCursor.first();
                Category cat = mCursor.moveRelative(position);

                final TextView name = (TextView) view.findViewById(android.R.id.text1);
                name.setText(cat.getName());
            } catch (SQLException e) {
                throw new RuntimeException(e); // should not happen
            }

            return view;
        }

        public void setCategoryType(CategoryType type) {
            try {
                mCategoryType = type;
                setQuery(DbProvider.getHelper().getCategoryDao().queryBuilder()
                        .where().eq("type", mCategoryType).and().eq("deleted", false).prepare());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            notifyDataSetChanged();
        }

        public CategoryType getCategoryType() {
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
                    final UUID categoryID = mCategoriesAdapter.getItemUUID(mItemPosition);
                    final CategoryDialogFragment fragment = CategoryDialogFragment.forCategory(categoryID.toString());
                    fragment.show(getFragmentManager(), "categoryCreate");
                    break;
                case 1: // delete
                    final UUID categoryId = mCategoriesAdapter.getItemUUID(mItemPosition);
                    DbProvider.getHelper().getCategoryDao().deleteById(categoryId);
                    break;
            }
        }
    }
}
