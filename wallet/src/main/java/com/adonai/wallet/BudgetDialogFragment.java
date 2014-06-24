package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.Category;

/**
 * Dialog fragment showing window for budget modifying/adding
 *
 * @author adonai
 */
public class BudgetDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {
    private final static String BUDGET_REFERENCE = "budget.reference";

    private EditText mBudgetName;
    private EditText mStartDate;
    private EditText mEndDate;
    private Spinner mCoveredAccountSelector;

    private AccountsWithNoneAdapter mAccountAdapter;
    private Budget mBudget;

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
        mStartDate = (EditText) dialog.findViewById(R.id.start_date_picker_edit);
        mEndDate = (EditText) dialog.findViewById(R.id.end_date_picker_edit);

        mAccountAdapter = new AccountsWithNoneAdapter(R.string.all);
        mCoveredAccountSelector = (Spinner) dialog.findViewById(R.id.covered_account_spinner);
        mCoveredAccountSelector.setAdapter(mAccountAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(getArguments() != null && getArguments().containsKey(BUDGET_REFERENCE)) {
            mBudget = Budget.getFromDB(getArguments().getString(BUDGET_REFERENCE));

            builder.setPositiveButton(R.string.confirm, null);
            builder.setTitle(R.string.edit_budget).setView(dialog);

            mBudgetName.setText(mBudget.getName());
            mStartDate.setText(Utils.VIEW_DATE_FORMAT.format(mBudget.getStartTime()));
            mEndDate.setText(Utils.VIEW_DATE_FORMAT.format(mBudget.getEndTime()));
            if(mBudget.getCoveredAccount() != null)
                mCoveredAccountSelector.setSelection(mAccountAdapter.getPosition(mBudget.getCoveredAccount().getId()));
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
        if(mBudget != null) { // modifying existing budget
            mBudget.setName(mBudgetName.getText().toString());
            if(mCoveredAccountSelector.getSelectedItem() != null)
                mBudget.setCoveredAccount(Account.getFromDB(mAccountAdapter.getItemUUID(mCoveredAccountSelector.getSelectedItemPosition())));
            else if (mBudget.getCoveredAccount() != null)
                mBudget.setCoveredAccount(null);
            if(DatabaseDAO.getInstance().makeAction(DatabaseDAO.ActionType.MODIFY, mBudget))
                dismiss();
            else
                Toast.makeText(getActivity(), R.string.category_not_found, Toast.LENGTH_SHORT).show();
        } else { // new category
            final Budget tempBudget = new Budget();
            tempBudget.setName(mBudgetName.getText().toString());
            tempBudget.setStartTime(null);
            tempBudget.setEndTime(null);
            if(mCoveredAccountSelector.getSelectedItem() != null)
                tempBudget.setCoveredAccount(Account.getFromDB(mAccountAdapter.getItemUUID(mCoveredAccountSelector.getSelectedItemPosition())));
            if(DatabaseDAO.getInstance().makeAction(DatabaseDAO.ActionType.ADD, tempBudget))
                dismiss();
            else
                Toast.makeText(getActivity(), R.string.category_exists, Toast.LENGTH_SHORT).show();
        }
    }
}
