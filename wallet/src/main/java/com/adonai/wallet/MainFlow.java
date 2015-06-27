package com.adonai.wallet;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.adonai.wallet.database.DbProvider;

import java.util.ArrayList;
import java.util.List;

import static com.adonai.wallet.WalletConstants.ACCOUNT_SYNC_KEY;

/**
 * Main activity of wallet master
 *
 * @author Adonai
 */
public class MainFlow extends WalletBaseActivity {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private ListView mNavigationDrawer;
    private String[] mDrawerTitles;
    
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    
    private List<Fragment> mParts = new ArrayList<>(5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DbProvider.setHelper(this);
        setContentView(R.layout.activity_main_flow);

        mNavigationDrawer = (ListView) findViewById(R.id.navigation_drawer);
        mDrawerTitles = getResources().getStringArray(R.array.drawer_items);
        mNavigationDrawer.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_selectable_list_item, mDrawerTitles));
        mNavigationDrawer.setOnItemClickListener(new DrawItemClickListener());
        
        mParts.add(new AccountsFragment());
        mParts.add(new OperationsFragment());
        mParts.add(new CategoriesFragment());
        mParts.add(new BudgetsFragment());
        mParts.add(new StatisticsFragment());

        // Set up the drawer.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                onNavigationDrawerItemSelected(mNavigationDrawer.getCheckedItemPosition());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        onNavigationDrawerItemSelected(0);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        // Handle your other action bar items...
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
    public void onBackPressed() {
        if(!LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Utils.BACK_PRESSED))) {
            super.onBackPressed();
        }
    }

    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, mParts.get(position));
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DbProvider.releaseHelper();
    }

    private class DrawItemClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mNavigationDrawer.setItemChecked(position, true);
            setTitle(mDrawerTitles[position]);
            mDrawerLayout.closeDrawer(mNavigationDrawer);
        }
    }
}
