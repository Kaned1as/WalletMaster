package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Budget;

import java.util.UUID;

/**
 * Dialog fragment showing window for budget modifying/adding
 *
 * @author adonai
 */
public class BudgetDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {
    private final static String BUDGET_REFERENCE = "budget.reference";

    private EditText mBudgetName;
    private DatePickerListener mStartDate;
    private DatePickerListener mEndDate;
    private Spinner mCoveredAccountSelector;

    private AccountsWithNoneAdapter mAccountAdapter;

    public static BudgetDialogFragment forBudget(String budgetId) {
        final BudgetDialogFragment fragment = new BudgetDialogFragment();
        final Bundle args = new Bundle();
        args.putString(BUDGET_REFERENCE, budgetId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.budget_create_modify_dialog, null);
        assert dialog != null;

        mBudgetName = (EditText) dialog.findViewById(R.id.name_edit);
        final EditText startDate = (EditText) dialog.findViewById(R.id.start_date_picker_edit);
        mStartDate = DatePickerListener.wrap(startDate);
        final EditText endDate = (EditText) dialog.findViewById(R.id.end_date_picker_edit);
        mEndDate = DatePickerListener.wrap(endDate);

        mAccountAdapter = new AccountsWithNoneAdapter(R.string.all);
        mCoveredAccountSelector = (Spinner) dialog.findViewById(R.id.covered_account_spinner);
        mCoveredAccountSelector.setAdapter(mAccountAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(getArguments() != null && getArguments().containsKey(BUDGET_REFERENCE)) {
            Budget budget = DbProvider.getHelper().getBudgetDao().queryForId(UUID.fromString(getArguments().getString(BUDGET_REFERENCE)));

            builder.setPositiveButton(R.string.confirm, null);
            builder.setTitle(R.string.edit_budget).setView(dialog);

            mBudgetName.setText(budget.getName());
            mStartDate.setCalendar(budget.getStartTime());
            mEndDate.setCalendar(budget.getEndTime());
            if(budget.getCoveredAccount() != null)
                mCoveredAccountSelector.setSelection(mAccountAdapter.getPosition(budget.getCoveredAccount().getId()));
        } else {
            builder.setPositiveButton(R.string.create, null);
            builder.setTitle(R.string.create_new_account).setView(dialog);
        }

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        try {
            Budget tmp;
            if(getArguments() != null && getArguments().containsKey(BUDGET_REFERENCE)) {
                tmp = DbProvider.getHelper().getBudgetDao().queryForId(UUID.fromString(getArguments().getString(BUDGET_REFERENCE)));
            } else
                tmp = new Budget();

            tmp.setName(mBudgetName.getText().toString());
            if (mStartDate.getCalendar().getTimeInMillis() >= mEndDate.getCalendar().getTimeInMillis())
                throw new IllegalArgumentException(getString(R.string.end_date_must_be_after));

            tmp.setStartTime(mStartDate.getCalendar().getTime());
            tmp.setEndTime(mEndDate.getCalendar().getTime());

            if (mCoveredAccountSelector.getSelectedItem() != null)
                tmp.setCoveredAccount(DbProvider.getHelper().getAccountDao().queryForId(mAccountAdapter.getItemUUID(mCoveredAccountSelector.getSelectedItemPosition())));
            else if (tmp.getCoveredAccount() != null)
                tmp.setCoveredAccount(null);

            DbProvider.getHelper().getBudgetDao().createOrUpdate(tmp);
            dismiss();
        } catch (IllegalArgumentException iae) {
            Toast.makeText(getWalletActivity(), iae.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
