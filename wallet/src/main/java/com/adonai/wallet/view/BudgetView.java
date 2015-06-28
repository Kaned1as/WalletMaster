package com.adonai.wallet.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.adonai.wallet.AccountDialogFragment;
import com.adonai.wallet.BudgetItemDialogFragment;
import com.adonai.wallet.R;
import com.adonai.wallet.WalletBaseActivity;
import com.adonai.wallet.WalletBaseListFragment;
import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
import com.adonai.wallet.adapters.UUIDCursorAdapter;
import com.j256.ormlite.stmt.Where;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;

/**
 * View designed for showing budget and its child views
 *
 * @author Adonai
 */
public class BudgetView extends LinearLayout {

    public enum State {
        COLLAPSED,
        EXPANDED
    }

    private Budget mBudget;
    private State mState = State.COLLAPSED;

    private View mHeaderView;
    private ImageView mExpander;
    private LinearLayout mExpandedView;
    private RelativeLayout mTotalProgress;
    private RelativeLayout mDailyProgress;
    private TextView mSliceByDay;
    private View mFooter;
    private BudgetItemCursorAdapter mBudgetItemCursorAdapter;

    public BudgetView(Context context) {
        super(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        mHeaderView = inflater.inflate(R.layout.budget_list_item, this, true);
        mTotalProgress = (RelativeLayout) mHeaderView.findViewById(R.id.total_amount_progressbar);
        mDailyProgress = (RelativeLayout) mHeaderView.findViewById(R.id.daily_amount_progressbar);
        mSliceByDay = (TextView) mHeaderView.findViewById(R.id.slice_by_day);
        mExpandedView = (LinearLayout) mHeaderView.findViewById(R.id.budget_items_list);
        mExpander = (ImageView) mHeaderView.findViewById(R.id.expand_view);
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
        if(mBudget.getEndTime() != null) {
            endTime.setText(VIEW_DATE_FORMAT.format(mBudget.getEndTime()));
        } else {
            endTime.setText("?");
        }
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
        
        updateAmounts();

        if(mBudgetItemCursorAdapter == null) { // never expanded before
            try {
                Where<BudgetItem, UUID> where = DbProvider.getHelper().getEntityDao(BudgetItem.class).queryBuilder().where().eq("parent_budget", mBudget);
                mBudgetItemCursorAdapter = new BudgetItemCursorAdapter((Activity) getContext(), where);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            mFooter = LayoutInflater.from(getContext()).inflate(R.layout.listview_add_footer, mExpandedView, false);
            mFooter.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final BudgetItemDialogFragment budgetCreate = BudgetItemDialogFragment.forBudget(mBudget.getId().toString());
                    budgetCreate.show(((WalletBaseActivity) getContext()).getSupportFragmentManager(), "budgetCreate");
                }
            });
        }

        mExpandedView.removeAllViews();
        for (int i = 0; i < mBudgetItemCursorAdapter.getCount(); ++i) {
            final View budget = mBudgetItemCursorAdapter.getView(i, null, mExpandedView);
            final BudgetItem budgetItem = mBudgetItemCursorAdapter.getItem(i);
            mExpandedView.addView(budget);
            budget.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    alertDialog.setItems(R.array.entity_choice_common, new BudgetItemChoice(budgetItem))
                            .setTitle(R.string.select_action)
                            .create()
                            .show();
                    return true;
                }
            });
        }
        mExpandedView.addView(mFooter);

        mState = State.EXPANDED;
        updateDrawables();
    }

    private class BudgetItemChoice implements DialogInterface.OnClickListener {

        private final BudgetItem budgetItem;
        
        public BudgetItemChoice(BudgetItem budgetItem) {
            this.budgetItem = budgetItem;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0: // modify
                    BudgetItemDialogFragment budgetModify = BudgetItemDialogFragment.forBudgetItem(budgetItem.getId().toString());
                    budgetModify.show(((WalletBaseActivity) getContext()).getSupportFragmentManager(), "budgetModify");
                    break;
                case 1: // delete
                    DbProvider.getHelper().getBudgetItemDao().delete(budgetItem);
                    break;
            }
        }
    }
    
    private void updateAmounts() {
        mTotalProgress.setVisibility(VISIBLE);
        BigDecimal maxAmount = mBudget.getMaxAmount();
        BigDecimal currentAmount = mBudget.getCurrentAmount();
        
        ProgressBar totalProgress = (ProgressBar) mTotalProgress.findViewById(R.id.deplete_progress);
        TextView maxAmountText = (TextView) mTotalProgress.findViewById(R.id.max_amount_label);
        TextView totalAmountTitle = (TextView) mTotalProgress.findViewById(R.id.title_label);
        TextView currentAmountText = (TextView) mTotalProgress.findViewById(R.id.current_progress_label);
        totalAmountTitle.setText(R.string.total_amount);
        maxAmountText.setText(maxAmount.toPlainString());
        currentAmountText.setText(currentAmount.toPlainString());
        totalProgress.setMax(maxAmount.intValue());
        totalProgress.setProgress(currentAmount.intValue());
        
        if(mBudget.getMaxDailyAmount() != null) { // we have daily amount specified
            mDailyProgress.setVisibility(VISIBLE);
            BigDecimal maxDailyAmount = mBudget.getMaxDailyAmount();
            BigDecimal currentDailyAmount = mBudget.getCurrentDailyAmount();

            ProgressBar dailyProgress = (ProgressBar) mDailyProgress.findViewById(R.id.deplete_progress);
            TextView dailyAmountText = (TextView) mDailyProgress.findViewById(R.id.max_amount_label);
            TextView dailyAmountTitle = (TextView) mDailyProgress.findViewById(R.id.title_label);
            TextView currentDailyAmountText = (TextView) mDailyProgress.findViewById(R.id.current_progress_label);
            dailyAmountTitle.setText(R.string.daily_amount);
            dailyAmountText.setText(maxDailyAmount.toPlainString());
            currentDailyAmountText.setText(currentDailyAmount.toPlainString());
            dailyProgress.setMax(maxDailyAmount.intValue());
            dailyProgress.setProgress(currentDailyAmount.intValue());

            if(mBudget.getEndTime() != null) { // we have end time ,let's see slice
                long timeDiff = mBudget.getEndTime().getTime() - mBudget.getStartTime().getTime();
                long days = timeDiff / TimeUnit.DAYS.toMillis(1);
                long currentDayDiff = Calendar.getInstance().getTimeInMillis() - mBudget.getStartTime().getTime();
                long daysSinceStart = currentDayDiff / TimeUnit.DAYS.toMillis(1);
                if(days > 0 && daysSinceStart > 0) { // we both have budget more than on zero days and one day passed
                    mSliceByDay.setVisibility(VISIBLE);
                    BigDecimal amountForToday = mBudget.getMaxDailyAmount()
                            .multiply(new BigDecimal(daysSinceStart))
                            .subtract(currentAmount);
                    
                    /*BigDecimal amountForToday = mBudget.getMaxAmount()
                            .multiply(new BigDecimal(daysSinceStart))
                            .divide(new BigDecimal(days), 2, BigDecimal.ROUND_HALF_UP)
                            .subtract(currentAmount);*/

                    String sign;
                    if(amountForToday.signum() > 0) {
                        sign = "+";
                        mSliceByDay.setTextColor(getResources().getColor(R.color.green_amount));
                    } else {
                        sign = "-";
                        mSliceByDay.setTextColor(getResources().getColor(R.color.red_amount));
                    }
                    String sliceAmountForToday = sign + amountForToday.toPlainString();
                    mSliceByDay.setText(getResources().getString(R.string.slice_by_day) + ": " + sliceAmountForToday);
                }
            }
        }
    }
    public void collapse() {
        if(mBudget == null) // no budget - no collapsing (should not happen)
            return;

        mTotalProgress.setVisibility(GONE);
        mDailyProgress.setVisibility(GONE);
        mSliceByDay.setVisibility(GONE);

        if(mBudgetItemCursorAdapter != null) { // was expanded before, unregister
            mBudgetItemCursorAdapter.closeCursor();
            mBudgetItemCursorAdapter = null;
        }

        mState = State.COLLAPSED;
        mExpandedView.removeAllViews();
        updateDrawables();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        collapse(); // close child cursor
    }

    private void updateDrawables() {
        setBackgroundColor(getContext().getResources().getColor(mState == State.COLLAPSED ? android.R.color.transparent : R.color.expanded_budget_item_bg));

        TypedArray attr = getContext().getTheme().obtainStyledAttributes(new int[]{mState == State.COLLAPSED ? R.attr.ExpandBudgetDrawable : R.attr.CollapseBudgetDrawable});
        int attributeResourceId = attr.getResourceId(0, 0);
        mExpander.setImageResource(attributeResourceId);
        attr.recycle();
    }

    public class BudgetItemCursorAdapter extends UUIDCursorAdapter<BudgetItem> {

        public BudgetItemCursorAdapter(Activity context, Where<BudgetItem, UUID> where) {
            super(context, BudgetItem.class, where);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            final LayoutInflater inflater = LayoutInflater.from(mContext);

            if (convertView == null)
                view = inflater.inflate(R.layout.progressbar_with_counters, parent, false);
            else
                view = convertView;

            try {
                mCursor.first();
                final BudgetItem bItem = mCursor.moveRelative(position);
                bItem.setParentBudget(mBudget);
                final TextView categoryText = (TextView) view.findViewById(R.id.title_label);
                categoryText.setText(bItem.getCategory().getName());
                final TextView maxAmountText = (TextView) view.findViewById(R.id.max_amount_label);
                maxAmountText.setText(bItem.getMaxAmount().toPlainString());
                final BigDecimal currentProgress = bItem.getProgress(); // invokes DB operation, be careful!
                final TextView currentAmountText = (TextView) view.findViewById(R.id.current_progress_label);
                currentAmountText.setText(currentProgress.toPlainString());
                final ProgressBar progress = (ProgressBar) view.findViewById(R.id.deplete_progress);
                progress.setMax(bItem.getMaxAmount().intValue());
                progress.setProgress(currentProgress.intValue());
                if(currentProgress.compareTo(bItem.getMaxAmount()) > 0) { // that's very important!
                    Drawable redded = progress.getProgressDrawable().mutate();
                    redded.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    progress.setProgressDrawable(redded);
                } else {
                    progress.getProgressDrawable().clearColorFilter();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return view;
        }
    }
}
