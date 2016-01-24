package com.adonai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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

import com.adonai.wallet.database.AbstractAsyncLoader;
import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.adapters.UUIDCursorAdapter;
import com.j256.ormlite.android.AndroidDatabaseResults;

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
    private CategoryNavigator mNavListener;

    private RetrieveContentsCallback mContentRetrieveCallback = new RetrieveContentsCallback();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.categories_flow, container, false);
        assert rootView != null;

        final String[] categoryTypes = new String[] {getString(R.string.outcome), getString(R.string.income), getString(R.string.transfer)};
        mCategoryTypeAdapter = new ArrayAdapter<>(getActivity(), R.layout.tall_list_item, categoryTypes);
        mNavListener = new CategoryNavigator();

        mEntityList = (ListView) rootView.findViewById(R.id.categories_list);
        getLoaderManager().initLoader(Utils.CATEGORIES_LOADER, Bundle.EMPTY, mContentRetrieveCallback);
        
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.categories_flow, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        ActionBar actBar = getWalletActivity().getSupportActionBar();
        actBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actBar.setListNavigationCallbacks(mCategoryTypeAdapter, mNavListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        ActionBar actBar = getWalletActivity().getSupportActionBar();
        actBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_category:
                final CategoryDialogFragment fragment = CategoryDialogFragment.newInstance(mNavListener.selectedPos);
                fragment.show(getFragmentManager(), "categoryCreate");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class CategoryNavigator implements ActionBar.OnNavigationListener {
        
        private int selectedPos = CategoryType.EXPENSE.ordinal();

        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            selectedPos = itemPosition;
            getLoaderManager().getLoader(Utils.CATEGORIES_LOADER).onContentChanged();
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
                ((AndroidDatabaseResults) mCursor.getRawResults()).moveAbsolute(position);
                Category cat = mCursor.current();

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
            CategoriesAdapter adapter = (CategoriesAdapter) mEntityList.getAdapter();
            switch (which) {
                case 0: // modify
                    final UUID categoryID = adapter.getItemUUID(mItemPosition);
                    final CategoryDialogFragment fragment = CategoryDialogFragment.forCategory(categoryID.toString());
                    fragment.show(getFragmentManager(), "categoryCreate");
                    break;
                case 1: // delete
                    final UUID categoryId = adapter.getItemUUID(mItemPosition);
                    DbProvider.getHelper().getCategoryDao().deleteById(categoryId);
                    break;
            }
        }
    }

    private class RetrieveContentsCallback implements LoaderManager.LoaderCallbacks<CategoriesAdapter> {
        @Override
        public Loader<CategoriesAdapter> onCreateLoader(int id, @NonNull final Bundle args) {
            AbstractAsyncLoader<CategoriesAdapter> toRegister = new AbstractAsyncLoader<CategoriesAdapter>(getActivity()) {
                @Nullable
                @Override
                public CategoriesAdapter loadInBackground() {
                    if(!isStarted()) // task was cancelled
                        return null;

                    // check the DB for accounts
                    CategoryType type = CategoryType.values()[mNavListener.selectedPos];
                    return new CategoriesAdapter(getActivity(), R.layout.category_list_item, type);
                }

                @Override
                protected void onForceLoad() {
                    if(mData != null) { // close old adapter before loading new one
                        mData.closeCursor();
                    }
                    super.onForceLoad();
                }
            };
            EntityDao<Category> accDao = DbProvider.getHelper().getDao(Category.class);
            accDao.registerObserver(toRegister);
            return toRegister;
        }

        @Override
        public void onLoadFinished(Loader<CategoriesAdapter> loader, CategoriesAdapter data) {
            mEntityList.setAdapter(data);
            mEntityList.setOnItemLongClickListener(new CategoryEditListener());
        }

        @Override
        public void onLoaderReset(Loader<CategoriesAdapter> loader) {
        }

    }
}
