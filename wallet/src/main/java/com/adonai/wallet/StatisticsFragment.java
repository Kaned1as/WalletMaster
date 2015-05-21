package com.adonai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.adonai.wallet.entities.UUIDCursorAdapter;

import java.sql.SQLException;
import java.util.UUID;

import static com.adonai.wallet.entities.Category.CategoryType;

/**
 * Fragment that is responsible for showing various statistics
 * and their context actions
 *
 * @author Adonai
 */
public class StatisticsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View rootView = inflater.inflate(R.layout.statistics_flow, container, false);
        assert rootView != null;

        return rootView;
    }
}
