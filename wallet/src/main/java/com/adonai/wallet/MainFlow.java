package com.adonai.wallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.adonai.wallet.database.DbProvider;

import java.util.ArrayList;
import java.util.List;

import static com.adonai.wallet.WalletConstants.ACCOUNT_SYNC_KEY;

/**
 * Main activity of wallet master
 *
 * @author Adonai
 */
public class MainFlow extends WalletBaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private List<Fragment> mParts = new ArrayList<>(4);

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DbProvider.setHelper(this);
        setContentView(R.layout.activity_main_flow);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mParts.add(getSupportFragmentManager().findFragmentById(R.id.accounts_fragment));
        mParts.add(getSupportFragmentManager().findFragmentById(R.id.operations_fragment));
        mParts.add(getSupportFragmentManager().findFragmentById(R.id.categories_fragment));
        mParts.add(getSupportFragmentManager().findFragmentById(R.id.budgets_fragment));
        mParts.add(getSupportFragmentManager().findFragmentById(R.id.statistics_fragment));

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onBackPressed() {
        if(!LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Utils.BACK_PRESSED))) {
            super.onBackPressed();
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for(final Fragment fragment : mParts)
            transaction.hide(fragment);
        switch (position) {
            case 0:
                mTitle = getString(R.string.title_accounts);
                break;
            case 1:
                mTitle = getString(R.string.title_operations);
                break;
            case 2:
                mTitle = getString(R.string.categories);
                break;
            case 3:
                mTitle = getString(R.string.title_budget);
                break;
            case 4:
                mTitle = getString(R.string.title_statistics);
                break;
        }
        transaction.show(mParts.get(position));
        transaction.commit();
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            //getMenuInflater().inflate(R.menu.accounts_flow, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_settings: {
                final Intent pref = new Intent(this, PreferenceFlow.class);
                startActivity(pref);
                break;
            }
            case R.id.action_sync: {
                if(mPreferences.contains(ACCOUNT_SYNC_KEY)) // have already configured sync account previously...
                    startSync();
                else // need to configure now!
                    new SyncDialogFragment().show(getSupportFragmentManager(), "syncAcc");
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DbProvider.releaseHelper();
    }
}
