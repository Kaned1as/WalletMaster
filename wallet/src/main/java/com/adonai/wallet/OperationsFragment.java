package com.adonai.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author adonai
 */
public class OperationsFragment extends WalletBaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.operations_flow, container, false);
        assert rootView != null;

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.operations_flow, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_operation:
                final OperationDialogFragment opCreate = new OperationDialogFragment();
                opCreate.show(getFragmentManager(), "opCreate");
                break;
            default :
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
