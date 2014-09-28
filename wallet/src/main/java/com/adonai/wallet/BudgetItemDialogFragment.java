package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Budget;
import com.adonai.wallet.entities.BudgetItem;
import com.adonai.wallet.entities.Category;

import java.math.BigDecimal;
import java.util.UUID;

import static com.adonai.wallet.CategoriesFragment.CategoriesAdapter;
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
        mCategoryAdapter = new CategoriesAdapter(getActivity(), Category.CategoryType.EXPENSE);
        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mCategorySelector.setAdapter(mCategoryAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // if we are modifying existing account
        if(getArguments().containsKey(BUDGET_ITEM_REFERENCE)) {
            BudgetItem bi = DbProvider.getHelper().getBudgetItemDao().queryForId(UUID.fromString(getArguments().getString(BUDGET_ITEM_REFERENCE)));

            builder.setPositiveButton(R.string.confirm, null);
            builder.setTitle(R.string.edit_budget_item).setView(dialog);

            mMaxAmountEdit.setText(bi.getMaxAmount().toPlainString());
            mCategorySelector.setSelection(mCategoryAdapter.getPosition(bi.getCategory().getId()));
        } else {
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

        if(getArguments().containsKey(BUDGET_ITEM_REFERENCE)) { // modifying existing budget item
            BudgetItem bi = DbProvider.getHelper().getBudgetItemDao().queryForId(UUID.fromString(getArguments().getString(BUDGET_ITEM_REFERENCE)));
            bi.setMaxAmount(amount);
            bi.setCategory(DbProvider.getHelper().getCategoryDao().queryForId(mCategoryAdapter.getItemUUID(mCategorySelector.getSelectedItemPosition())));
            DbProvider.getHelper().getBudgetItemDao().update(bi);
            dismiss();
        } else { // new budget item
            Budget budget = DbProvider.getHelper().getBudgetDao().queryForId(UUID.fromString(getArguments().getString(BUDGET_REFERENCE)));
            final BudgetItem tempBudget = new BudgetItem(budget);
            tempBudget.setMaxAmount(amount);
            tempBudget.setCategory(DbProvider.getHelper().getCategoryDao().queryForId(mCategoryAdapter.getItemUUID(mCategorySelector.getSelectedItemPosition())));
            DbProvider.getHelper().getBudgetItemDao().create(tempBudget);
            dismiss();
        }
    }
}
