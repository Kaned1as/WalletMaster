package com.adonai.wallet.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.adonai.wallet.BudgetItemDialogFragment;
import com.adonai.wallet.BudgetsFragment;
import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.R;
import com.adonai.wallet.Utils;
import com.adonai.wallet.WalletBaseActivity;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
import com.adonai.wallet.entities.UUIDCursorAdapter;

import static com.adonai.wallet.DatabaseDAO.BudgetItemFields;
import static com.adonai.wallet.DatabaseDAO.EntityType.BUDGET_ITEMS;
import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;

/**
 * Created by adonai on 12.06.14.
 */
public class BudgetView extends LinearLayout {

    public enum State {
        COLLAPSED,
        EXPANDED
    }

    private final BudgetsFragment.BudgetsAdapter mViewAdapter;

    private Budget mBudget;
    private State mState = State.COLLAPSED;

    private View mCollapsedView;
    private ImageView mExpander;
    private ListView mExpandedView;
    private BudgetItemCursorAdapter mBudgetItemCursorAdapter;

    public BudgetView(Context context, BudgetsFragment.BudgetsAdapter budgetsAdapter) {
        super(context);
        mViewAdapter = budgetsAdapter;

        setLayoutTransition(new LayoutTransition());
        final LayoutInflater inflater = LayoutInflater.from(context);
        mCollapsedView = inflater.inflate(R.layout.budget_list_item, this, true);
        mExpandedView = (ListView) mCollapsedView.findViewById(R.id.budget_items_list);
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

    public State getState() {
        return mState;
    }

    public void setBudget(Budget budget) {
        final Budget oldBudget = this.mBudget;

        this.mBudget = budget;
        if(oldBudget == null || !oldBudget.equals(mBudget))
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

        collapse();
    }

    public void expand() {
        if(mBudget == null) // no budget - no expanding (should not happen)
            return;

        if(mExpandedView.getAdapter() == null) { // never expanded before
            mBudgetItemCursorAdapter = new BudgetItemCursorAdapter(getContext());
            mExpandedView.setAdapter(mBudgetItemCursorAdapter);

            final View footer = View.inflate(getContext(), R.layout.listview_add_footer, null);
            mExpandedView.addFooterView(footer);
            footer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final BudgetItemDialogFragment budgetCreate = BudgetItemDialogFragment.forBudget(mBudget.getId());
                    budgetCreate.show(((WalletBaseActivity) getContext()).getFragmentManager(), "budgetCreate");
                }
            });
        }

        DatabaseDAO.getInstance().registerDatabaseListener(mBudgetItemCursorAdapter, null);
        mBudgetItemCursorAdapter.handleUpdate();

        mExpandedView.getLayoutParams().height = (int) Utils.convertDpToPixel(200f, getContext());
        mExpandedView.setLayoutParams(mExpandedView.getLayoutParams());
        mState = State.EXPANDED;
        mViewAdapter.addExpandedView(this);
        updateExpanderDrawable();
    }

    public void collapse() {
        if(mBudget == null) // no budget - no expanding (should not happen)
            return;

        if(mBudgetItemCursorAdapter != null) { // was expanded before, unregister
            DatabaseDAO.getInstance().unregisterDatabaseListener(mBudgetItemCursorAdapter, null);
            mBudgetItemCursorAdapter.changeCursor(null);

            mExpandedView.getLayoutParams().height = 0;
            mExpandedView.setLayoutParams(mExpandedView.getLayoutParams());
            mState = State.COLLAPSED;
            mViewAdapter.removeExpandedView(this);
            updateExpanderDrawable();
        }
    }

    private void updateExpanderDrawable() {
        TypedArray attr = getContext().getTheme().obtainStyledAttributes(new int[]{mState == State.COLLAPSED ? R.attr.ExpandBudgetDrawable : R.attr.CollapseBudgetDrawable});
        int attributeResourceId = attr.getResourceId(0, 0);
        mExpander.setImageResource(attributeResourceId);
        attr.recycle();
    }

    public class BudgetItemCursorAdapter extends UUIDCursorAdapter implements DatabaseDAO.DatabaseListener {

        public BudgetItemCursorAdapter(Context context) {
            super(context, null);
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

        @Override
        public void handleUpdate() {
            ((WalletBaseActivity) getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeCursor(DatabaseDAO.getInstance().getCustomCursor(BUDGET_ITEMS, BudgetItemFields.PARENT_BUDGET.toString(), mBudget.getId()));
                }
            });
        }
    }


}
