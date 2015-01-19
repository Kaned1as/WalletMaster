package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Operation;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.adonai.wallet.entities.UUIDSpinnerAdapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.adonai.wallet.CategoriesFragment.CategoriesAdapter;
import static com.adonai.wallet.Utils.getValue;

/**
 * Dialog fragment for operation deleting/modifying/creating
 *
 * @author adonai
 */
public class OperationDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener, DialogInterface.OnClickListener {

    private static final String OPERATION_REFERENCE = "operation.reference";
    private static final String ACCOUNT_REFERENCE = "account.reference";

    private EditText mDescription;

    private DatePickerListener mDatePicker;
    private ImageButton mCategoryAddButton;

    private Spinner mChargeAccountSelector;
    private Spinner mCategorySelector;
    private EditText mAmount;

    private RadioGroup mTypeSwitch;

    private final List<TableRow> conversionRows = new ArrayList<>();
    private TableRow mBeneficiarRow;
    private TableRow mChargerRow;

    private Spinner mBeneficiarAccountSelector;
    private EditText mBeneficiarConversionRate;
    private TextView mBeneficiarAmountDelivered;

    private CategoriesAdapter mCategoriesAdapter;
    private UUIDSpinnerAdapter<Account> mAccountAdapter;

    public OperationDialogFragment() {
        //super();
    }

    public static OperationDialogFragment forOperation(String operationId) {
        final OperationDialogFragment fragment = new OperationDialogFragment();
        final Bundle args = new Bundle();
        args.putString(OPERATION_REFERENCE, operationId);
        fragment.setArguments(args);
        return fragment;
    }

    public static OperationDialogFragment forAccount(String accountId) {
        final OperationDialogFragment fragment = new OperationDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ACCOUNT_REFERENCE, accountId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAccountAdapter = new UUIDSpinnerAdapter<>(getActivity(), DbProvider.getHelper().getEntityDao(Account.class));
        final AccountSelectListener accountSelectListener = new AccountSelectListener();
        final CountChangedWatcher textWatcher = new CountChangedWatcher();

        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.operation_create_modify_dialog, null);
        assert dialog != null;

        mDescription = (EditText) dialog.findViewById(R.id.description_edit);
        final EditText datePicker = (EditText) dialog.findViewById(R.id.date_picker_edit);
        mDatePicker = DatePickerListener.wrap(datePicker);

        mChargeAccountSelector = (Spinner) dialog.findViewById(R.id.charge_account_spinner);
        mChargeAccountSelector.setAdapter(mAccountAdapter);
        mChargeAccountSelector.setOnItemSelectedListener(accountSelectListener);

        mCategoriesAdapter = new CategoriesAdapter(getActivity(), android.R.layout.simple_spinner_item, Category.CategoryType.EXPENSE);

        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mCategorySelector.setOnItemSelectedListener(new CategorySelectListener());
        mCategorySelector.setAdapter(mCategoriesAdapter);
        mCategoryAddButton = (ImageButton) dialog.findViewById(R.id.category_add_button);
        mCategoryAddButton.setOnClickListener(this);

        mAmount = (EditText) dialog.findViewById(R.id.amount_edit);
        mAmount.addTextChangedListener(textWatcher);

        mTypeSwitch = (RadioGroup) dialog.findViewById(R.id.operation_type_switch);
        mTypeSwitch.setOnCheckedChangeListener(new TypeSelector());

        mChargerRow = (TableRow) dialog.findViewById(R.id.operation_charge_row);
        mBeneficiarRow = (TableRow) dialog.findViewById(R.id.operation_beneficiar_row);
        conversionRows.add((TableRow) dialog.findViewById(R.id.beneficiar_conversion));
        conversionRows.add((TableRow) dialog.findViewById(R.id.beneficiar_amount));

        mBeneficiarAccountSelector = (Spinner) dialog.findViewById(R.id.beneficiar_account_spinner);
        mBeneficiarAccountSelector.setAdapter(mAccountAdapter);
        mBeneficiarAccountSelector.setOnItemSelectedListener(accountSelectListener);

        mBeneficiarConversionRate = (EditText) dialog.findViewById(R.id.beneficiar_conversion_edit);
        mBeneficiarConversionRate.addTextChangedListener(textWatcher);

        mBeneficiarAmountDelivered = (TextView) dialog.findViewById(R.id.beneficiar_amount_value_label);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);

        // if we are modifying existing operation
        if(getArguments() != null && getArguments().containsKey(OPERATION_REFERENCE)) {
            UUID opId = UUID.fromString(getArguments().getString(OPERATION_REFERENCE));
            Operation tmpOp = DbProvider.getHelper().getOperationDao().queryForId(opId);
            builder.setPositiveButton(R.string.modify, this);
            builder.setTitle(R.string.edit_operation);
            mDescription.setText(tmpOp.getDescription());
            mAmount.setText(tmpOp.getAmount().toPlainString());
            mDatePicker.setCalendar(tmpOp.getTime());
            switch (tmpOp.getCategory().getType()) {
                case TRANSFER:
                    mTypeSwitch.check(R.id.transfer_radio);
                    mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(tmpOp.getOrderer().getId()));
                    mBeneficiarAccountSelector.setSelection(mAccountAdapter.getPosition(tmpOp.getBeneficiar().getId()));
                    mCategorySelector.setSelection(mCategoriesAdapter.getPosition(tmpOp.getCategory().getId()));
                    if(tmpOp.getConvertingRate() != null)
                        mBeneficiarConversionRate.setText(tmpOp.getConvertingRate().toString());
                    break;
                case EXPENSE:
                    mTypeSwitch.check(R.id.expense_radio);
                    mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(tmpOp.getOrderer().getId()));
                    mCategorySelector.setSelection(mCategoriesAdapter.getPosition(tmpOp.getCategory().getId()));
                    break;
                case INCOME:
                    mTypeSwitch.check(R.id.income_radio);
                    mBeneficiarAccountSelector.setSelection(mAccountAdapter.getPosition(tmpOp.getBeneficiar().getId()));
                    mCategorySelector.setSelection(mCategoriesAdapter.getPosition(tmpOp.getCategory().getId()));
                    break;
            }
        } else { // this is new operation
            builder.setPositiveButton(R.string.create, this);
            builder.setTitle(R.string.create_new_operation);
            mTypeSwitch.check(R.id.expense_radio);
            if(getArguments() != null && getArguments().containsKey(ACCOUNT_REFERENCE))
                mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(getArguments().getString(ACCOUNT_REFERENCE)));
        }

        return builder.create();
    }

    private class CategorySelectListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final UUID categoryID = ((UUIDCursorAdapter) mCategorySelector.getAdapter()).getItemUUID(position);
            final Category cat = DbProvider.getHelper().getCategoryDao().queryForId(categoryID);
            if (cat.getPreferredAccount() != null)  { // selected category has preferred account
                final UUID accId = cat.getPreferredAccount().getId();
                final int accPosition = mAccountAdapter.getPosition(accId); // get position
                if(position != -1) {
                    mChargeAccountSelector.setSelection(accPosition);
                    mBeneficiarAccountSelector.setSelection(accPosition);
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class AccountSelectListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateTransferConversionVisibility();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    // show conversion rows on transfer when currencies are different
    private void updateTransferConversionVisibility() {
        // TODO: rewrite all getSelectedItemPosition with getItem.getString(1)
        final UUID chargerID = mAccountAdapter.getItemUUID(mChargeAccountSelector.getSelectedItemPosition());
        final UUID beneficiarID = mAccountAdapter.getItemUUID(mBeneficiarAccountSelector.getSelectedItemPosition());
        final Account chargeAcc = DbProvider.getHelper().getAccountDao().queryForId(chargerID);
        final Account beneficiarAcc = DbProvider.getHelper().getAccountDao().queryForId(beneficiarID);
        if(mTypeSwitch.getCheckedRadioButtonId() == R.id.transfer_radio && !chargeAcc.getCurrency().equals(beneficiarAcc.getCurrency())) {
            for(TableRow row : conversionRows)
                row.setVisibility(View.VISIBLE);

            updateConversionAmount();
        } else
            for(TableRow row : conversionRows)
                row.setVisibility(View.GONE);
    }

    public void updateConversionAmount() {
        final BigDecimal amount = getValue(mAmount.getText().toString(), BigDecimal.ZERO);
        final Double rate = getValue(mBeneficiarConversionRate.getText().toString(), 1d);
        final BigDecimal beneficiarAmount = amount.divide(BigDecimal.valueOf(rate), 2, RoundingMode.HALF_UP);
        mBeneficiarAmountDelivered.setText(beneficiarAmount.toPlainString());
    }

    private class TypeSelector implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.transfer_radio: // on transfer we don't need category but have to specify beneficiar account
                    mBeneficiarRow.setVisibility(View.VISIBLE);
                    mChargerRow.setVisibility(View.VISIBLE);

                    mAmount.setTextColor(Color.parseColor("#0000AA"));
                    mCategoriesAdapter.setCategoryType(Category.CategoryType.TRANSFER);

                    updateTransferConversionVisibility();
                    break;
                case R.id.income_radio: // on income we need category with type=income
                    mBeneficiarRow.setVisibility(View.VISIBLE);
                    mChargerRow.setVisibility(View.GONE);

                    mCategoriesAdapter.setCategoryType(Category.CategoryType.INCOME);

                    mAmount.setTextColor(Color.parseColor("#00AA00"));
                    break;
                case R.id.expense_radio: // on expense we need category with type=expense
                    mBeneficiarRow.setVisibility(View.GONE);
                    mChargerRow.setVisibility(View.VISIBLE);

                    mCategoriesAdapter.setCategoryType(Category.CategoryType.EXPENSE);

                    mAmount.setTextColor(Color.parseColor("#AA0000"));
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.category_add_button: {
                final CategoryDialogFragment fragment = CategoryDialogFragment.newInstance(mCategoriesAdapter.getCategoryType().ordinal());
                fragment.setOnCategoryCreateListener(new CategoryDialogFragment.OnCategoryCreateListener() {
                    @Override
                    public void handleCategoryCreate(String categoryId) {
                        mCategorySelector.setSelection(mCategoriesAdapter.getPosition(categoryId));
                    }
                });
                fragment.show(getFragmentManager(), "categoryCreate");
                break;
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        try {
            if (getArguments() != null && getArguments().containsKey(OPERATION_REFERENCE)) { // operation is already applied, need to revert it and re-apply again
                UUID revertId = UUID.fromString(getArguments().getString(OPERATION_REFERENCE));
                Operation revertOp = DbProvider.getHelper().getOperationDao().queryForId(revertId);
                if (Operation.revertOperation(revertOp)) { // revert original one and apply ours
                    fillOperationFields(revertOp);
                    Operation.applyOperation(revertOp);
                    dismiss();
                }
                else
                    throw new IllegalStateException("Cannot reapply operation!"); // should never happen!!
            } else { // create new operation with data from fields specified
                Operation toCreate = new Operation();
                fillOperationFields(toCreate);
                if (Operation.applyOperation(toCreate)) // all succeeded
                    dismiss();
                else
                    throw new IllegalStateException("Cannot apply operation!"); // should never happen!!
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException ex) {
            Toast.makeText(getWalletActivity(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fillOperationFields(Operation fillOp) {
        // check data input
        final BigDecimal amount = getValue(mAmount.getText().toString(), BigDecimal.ZERO);
        if (amount.equals(BigDecimal.ZERO))
            throw new IllegalArgumentException(getString(R.string.operation_needs_amount));

        final UUID opCategoryID = ((UUIDCursorAdapter) mCategorySelector.getAdapter()).getItemUUID(mCategorySelector.getSelectedItemPosition());
        final Category opCategory = DbProvider.getHelper().getCategoryDao().queryForId(opCategoryID);
        if (opCategory == null)
            throw new IllegalArgumentException(getString(R.string.operation_needs_category));
        // fill operation fields
       fillOp.setAmount(amount);
       fillOp.setCategory(opCategory);
       fillOp.setTime(mDatePicker.getCalendar().getTime());
       fillOp.setDescription(mDescription.getText().toString());
        switch (mTypeSwitch.getCheckedRadioButtonId()) { // prepare operation depending on type
            case R.id.transfer_radio: { // transfer op, we need 2 accounts
                final UUID chargerID = mAccountAdapter.getItemUUID(mChargeAccountSelector.getSelectedItemPosition());
                final UUID beneficiarID = mAccountAdapter.getItemUUID(mBeneficiarAccountSelector.getSelectedItemPosition());
                final Account chargeAcc = DbProvider.getHelper().getAccountDao().queryForId(chargerID);
                final Account benefAcc = DbProvider.getHelper().getAccountDao().queryForId(beneficiarID);
                if (chargeAcc == null || benefAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_transfer_accs));
                if (chargeAcc.getId().equals(benefAcc.getId())) // no transfer to self
                    throw new IllegalArgumentException(getString(R.string.accounts_identical));
                fillOp.setOrderer(chargeAcc);
                fillOp.setBeneficiar(benefAcc);
                if (!fillOp.getBeneficiar().getCurrency().equals(fillOp.getOrderer().getCurrency())) // different currencies
                    fillOp.setConvertingRate(getValue(mBeneficiarConversionRate.getText().toString(), BigDecimal.ONE));
                break;
            }
            case R.id.income_radio: { // income op, we need beneficiar
                final UUID beneficiarID = mAccountAdapter.getItemUUID(mBeneficiarAccountSelector.getSelectedItemPosition());
                final Account benefAcc = DbProvider.getHelper().getAccountDao().queryForId(beneficiarID);
                if (benefAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_acc));
                fillOp.setBeneficiar(benefAcc);
                fillOp.setOrderer(null);
                break;
            }
            case R.id.expense_radio: { // expense op, we need charger
                final UUID chargerID = mAccountAdapter.getItemUUID(mChargeAccountSelector.getSelectedItemPosition());
                final Account chargeAcc = DbProvider.getHelper().getAccountDao().queryForId(chargerID);
                if (chargeAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_acc));
                fillOp.setOrderer(chargeAcc);
                fillOp.setBeneficiar(null);
                break;
            }
        }
    }

    private class CountChangedWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(getValue(mBeneficiarConversionRate.getText().toString(), 1d) == 0d) { // do not divide by zero
                mBeneficiarConversionRate.setText("1");
                return; // update will be called on pending callback
            }

            updateConversionAmount();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCategoriesAdapter.closeCursor();
    }
}
