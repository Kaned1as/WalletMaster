package com.adonai.wallet;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

/**
 * Fragment that is responsible for showing categories list
 * and their context actions
 *
 * @author adonai
 */
public class CategoriesFragment extends WalletBaseFragment {

    private SpinnerAdapter mSpinnerAdapter;
    private ActionBar.OnNavigationListener mNavListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.categories_flow, container, false);
        assert rootView != null;

        final String[] categoryTypes = new String[] {getString(R.string.expense), getString(R.string.income), getString(R.string.transfer)};
        mSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, categoryTypes);

        return rootView;
    }

    public SpinnerAdapter getActionBarAdapter() {
        return mSpinnerAdapter;
    }

    public ActionBar.OnNavigationListener getNavigationListener() {
        return mNavListener;
    }
}
