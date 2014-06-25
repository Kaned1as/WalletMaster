package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
import com.adonai.wallet.entities.Category;

import java.math.BigDecimal;

import static com.adonai.wallet.Utils.getValue;

/**
 * Dialog fragment showing window for budget modifying/adding
 *
 * @author adonai
 */
public class BudgetItemDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {
    private final static String BUDGET_ITEM_REFERENCE = "budget.item.reference";
    private final static String BUDGET_REFERENCE = "budget.reference";

    private Spinner mCategorySelector;
    private EditText mMaxAmountEdit;

    private CategoriesAdapter mCategoryAdapter;

    private Budget mParent;
    private BudgetItem mBudgetItem;

    public static BudgetItemDialogFragment forBudgetItem(String budgetItemId) {
        final BudgetItemDialogFragment fragment = new BudgetItemDialogFragment();
        final Bundle args = new Bundle();
        args.putString(BUDGET_ITEM_REFERENCE, budgetItemId);
        fragment.setArguments(args);
        return fragment;
    }

    public static BudgetItemDialogFragment forBudget(String budgetId) {
        final BudgetItemDialogFragment fragment = new BudgetItemDialogFragment();
        final Bundle args = new Bundle();
        args.putString(BUDGET_REFERENCE, budgetId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(getArguments() == null || !getArguments().containsKey(BUDGET_REFERENCE) && !getArguments().containsKey(BUDGET_ITEM_REFERENCE))
            throw new IllegalArgumentException("Cannot create budget item without parent reference!");

        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.budget_item_create_modify_dialog, null);
        assert dialog != null;

        mMaxAmountEdit = (EditText) dialog.findViewById(R.id.max_amount_edit);
        mCategoryAdapter = new CategoriesAdapter(Category.EXPENSE);
        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mCategorySelector.setAdapter(mCategoryAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(getArguments().containsKey(BUDGET_ITEM_REFERENCE)) {
            mBudgetItem = BudgetItem.getFromDB(getArguments().getString(BUDGET_ITEM_REFERENCE));

            builder.setPositiveButton(R.string.confirm, null);
            builder.setTitle(R.string.edit_budget_item).setView(dialog);

            mMaxAmountEdit.setText(mBudgetItem.getMaxAmount().toPlainString());
            mCategorySelector.setSelection(mCategoryAdapter.getPosition(mBudgetItem.getCategory().getId()));
        } else {
            mParent = Budget.getFromDB(getArguments().getString(BUDGET_REFERENCE));

            builder.setPositiveButton(R.string.create, null);
            builder.setTitle(R.string.create_new_budget_item).setView(dialog);
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
        final BigDecimal amount = getValue(mMaxAmountEdit.getText().toString(), BigDecimal.ZERO);

        if(mBudgetItem != null) { // modifying existing budget
            mBudgetItem.setMaxAmount(amount);
            mBudgetItem.setCategory(Category.getFromDB(mCategoryAdapter.getItemUUID(mCategorySelector.getSelectedItemPosition())));
            if(DatabaseDAO.getInstance().makeAction(DatabaseDAO.ActionType.MODIFY, mBudgetItem))
                dismiss();
            else
                Toast.makeText(getActivity(), R.string.budget_item_not_found, Toast.LENGTH_SHORT).show();
        } else { // new category
            final BudgetItem tempBudget = new BudgetItem(mParent);
            tempBudget.setMaxAmount(amount);
            tempBudget.setCategory(Category.getFromDB(mCategoryAdapter.getItemUUID(mCategorySelector.getSelectedItemPosition())));
            if(DatabaseDAO.getInstance().makeAction(DatabaseDAO.ActionType.ADD, tempBudget))
                dismiss();
            else
                Toast.makeText(getActivity(), R.string.budget_item_exists, Toast.LENGTH_SHORT).show();
        }
    }
}
