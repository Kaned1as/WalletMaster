package com.adonai.wallet.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.R;
import com.adonai.wallet.Utils;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
import com.adonai.wallet.entities.Operation;
import com.adonai.wallet.entities.UUIDCursorAdapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.adonai.wallet.DatabaseDAO.BudgetItemFields;
import static com.adonai.wallet.DatabaseDAO.EntityType.*;
import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;
import static com.adonai.wallet.Utils.convertDpToPixel;
import static com.adonai.wallet.Utils.convertPixelsToDp;

/**
 * Created by adonai on 12.06.14.
 */
public class BudgetView extends LinearLayout {

    private enum State {
        COLLAPSED,
        EXPANDED
    }

    private Budget mBudget;
    private State mState = State.COLLAPSED;

    private View mCollapsedView;
    private ImageView mExpander;
    private ListView mExpandedView;

    public BudgetView(Context context) {
        super(context);
        setLayoutTransition(new LayoutTransition());
        final LayoutInflater inflater = LayoutInflater.from(context);
        mCollapsedView = inflater.inflate(R.layout.budget_list_item, this, true);
        mExpander = (ImageView) mCollapsedView.findViewById(R.id.expand_view);
        mExpander.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == State.COLLAPSED)
                    expand();
                else
                    collapse();
            }
        });
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
        if(mBudget == null) // no budget - no expanding (should not happen)
            return;

        if(mExpandedView == null) { // never expanded before
            mExpandedView = new ListView(getContext());
            mExpandedView.getLayoutParams().height = (int) Utils.convertDpToPixel(200f, getContext());
            mExpandedView.setAdapter(new BudgetItemCursorAdapter(getContext()));

            final View footer = View.inflate(getContext(), R.layout.listview_add_footer, null);
            mExpandedView.addFooterView(footer);
            footer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //BudgetItemDialogFragment.newInstance();
                }
            });
        }

        addView(mExpandedView);
        mState = State.EXPANDED;
        updateExpanderDrawable();
    }

    private void updateExpanderDrawable() {
        TypedArray attr = getContext().getTheme().obtainStyledAttributes(new int[]{mState == State.COLLAPSED ? R.attr.ExpandBudgetDrawable : R.attr.CollapseBudgetDrawable});
        int attributeResourceId = attr.getResourceId(0, 0);
        mExpander.setImageResource(attributeResourceId);
        attr.recycle();
    }

    public void collapse() {
        removeView(mExpandedView);
        mState = State.COLLAPSED;
        updateExpanderDrawable();
    }

    public class BudgetItemCursorAdapter extends UUIDCursorAdapter {

        public BudgetItemCursorAdapter(Context context) {
            super(context, DatabaseDAO.getInstance().getCustomCursor(BUDGET_ITEMS, BudgetItemFields.PARENT_BUDGET.toString(), mBudget.getId()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            mCursor.moveToPosition(position);
            final BudgetItem bItem = BudgetItem.getFromDB(mCursor.getString(BudgetItemFields._id.ordinal()));
            bItem.setParentBudget(mBudget);

            if (convertView == null)
                view = inflater.inflate(R.layout.budget_item_list_item, parent, false);
            else
                view = convertView;

            final TextView categoryText = (TextView) view.findViewById(R.id.category_label);
            categoryText.setText(bItem.getCategory().getName());
            final TextView maxAmountText = (TextView) view.findViewById(R.id.max_amount_label);
            maxAmountText.setText(bItem.getMaxAmount().toPlainString());
            final ProgressBar progress = (ProgressBar) view.findViewById(R.id.deplete_progress);
            progress.setMax(bItem.getMaxAmount().intValue());
            progress.setProgress(bItem.getProgress().intValue());

            return view;
        }
    }
}
