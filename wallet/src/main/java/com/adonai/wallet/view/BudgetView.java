package com.adonai.wallet.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.adonai.wallet.R;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.Operation;

import java.util.HashMap;
import java.util.Map;

import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;
import static com.adonai.wallet.Utils.convertPixelsToDp;

/**
 * Created by adonai on 12.06.14.
 */
public class BudgetView extends FrameLayout {

    private enum State {
        COLLAPSED,
        EXPANDED
    }

    private Budget mBudget;
    private State mState = State.COLLAPSED;

    private View mCollapsedView;

    public BudgetView(Context context) {
        super(context);
        setLayoutTransition(new LayoutTransition());
        final LayoutInflater inflater = LayoutInflater.from(context);
        mCollapsedView = inflater.inflate(R.layout.budget_list_item, this, true);
    }

    public void setBudget(Budget budget) {
        this.mBudget = budget;
        onBudgetChanged();
    }

    private void onBudgetChanged() {
        final TextView name = (TextView) findViewById(R.id.name_text);
        final TextView startTime = (TextView) findViewById(R.id.start_time_text);
        final TextView endTime = (TextView) findViewById(R.id.end_time_text);
        final TextView coveredAccount = (TextView) findViewById(R.id.covered_account_text);

        name.setText(mBudget.getName());
        startTime.setText(VIEW_DATE_FORMAT.format(mBudget.getStartTime()));
        endTime.setText(VIEW_DATE_FORMAT.format(mBudget.getEndTime()));
        if(mBudget.getCoveredAccount() != null)
            coveredAccount.setText(mBudget.getCoveredAccount().getName());
        else
            coveredAccount.setText(getResources().getString(R.string.all));
        name.setText(mBudget.getName());
    }

    public void expand() {
        removeView(mCollapsedView);
    }
}
