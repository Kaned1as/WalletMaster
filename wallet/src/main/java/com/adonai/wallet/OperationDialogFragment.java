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

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Operation;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.adonai.wallet.entities.UUIDSpinnerAdapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.adonai.wallet.Utils.getValue;

/**
 * Dialog fragment for operation deleting/modifying/creating
 *
 * @author adonai
 */
public class OperationDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener, DialogInterface.OnClickListener {

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
    private UUIDSpinnerAdapter mAccountAdapter;

    private Operation mOperation;
    private Account mCharger;

    public OperationDialogFragment() {
        //super();
    }

    public OperationDialogFragment(Operation toModify) {
        mOperation = toModify;
    }

    public OperationDialogFragment(Account managed) {
        mCharger = managed;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAccountAdapter = new UUIDSpinnerAdapter(getActivity(), DatabaseDAO.getInstance().getAccountCursor());
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

        mCategoriesAdapter = new CategoriesAdapter(Category.EXPENSE);
        DatabaseDAO.getInstance().registerDatabaseListener(mCategoriesAdapter, DatabaseDAO.EntityType.CATEGORIES.toString());

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
        if(mOperation != null) {
            builder.setPositiveButton(R.string.modify, this);
            builder.setTitle(R.string.edit_operation);
            mDescription.setText(mOperation.getDescription());
            mAmount.setText(mOperation.getAmount().toPlainString());
            mDatePicker.setCalendar(mOperation.getTime());
            switch (mOperation.getOperationType()) {
                case TRANSFER:
                    mTypeSwitch.check(R.id.transfer_radio);
                    mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getCharger().getId()));
                    mBeneficiarAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getBeneficiar().getId()));
                    mCategorySelector.setSelection(mCategoriesAdapter.getPosition(mOperation.getCategory().getId()));
                    if(mOperation.getConvertingRate() != null)
                        mBeneficiarConversionRate.setText(mOperation.getConvertingRate().toString());
                    break;
                case EXPENSE:
                    mTypeSwitch.check(R.id.expense_radio);
                    mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getCharger().getId()));
                    mCategorySelector.setSelection(mCategoriesAdapter.getPosition(mOperation.getCategory().getId()));
                    break;
                case INCOME:
                    mTypeSwitch.check(R.id.income_radio);
                    mBeneficiarAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getBeneficiar().getId()));
                    mCategorySelector.setSelection(mCategoriesAdapter.getPosition(mOperation.getCategory().getId()));
                    break;
            }
        } else { // this is new operation
            builder.setPositiveButton(R.string.create, this);
            builder.setTitle(R.string.create_new_operation);
            mTypeSwitch.check(R.id.expense_radio);
            if(mCharger != null)
                mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(mCharger.getId()));
        }

        return builder.create();
    }

    private class CategorySelectListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final String categoryID = ((UUIDCursorAdapter) mCategorySelector.getAdapter()).getItemUUID(position);
            final Category cat = Category.getFromDB(categoryID);
            if (cat.getPreferredAccount() != null)  { // selected category has preferred account
                final String accId = cat.getPreferredAccount().getId();
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
        final String chargerID = mAccountAdapter.getItemUUID(mChargeAccountSelector.getSelectedItemPosition());
        final String beneficiarID = mAccountAdapter.getItemUUID(mBeneficiarAccountSelector.getSelectedItemPosition());
        final Account chargeAcc = Account.getFromDB(chargerID);
        final Account beneficiarAcc = Account.getFromDB(beneficiarID);
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
                    mCategoriesAdapter.setCategoryType(Category.TRANSFER);

                    updateTransferConversionVisibility();
                    break;
                case R.id.income_radio: // on income we need category with type=income
                    mBeneficiarRow.setVisibility(View.VISIBLE);
                    mChargerRow.setVisibility(View.GONE);

                    mCategoriesAdapter.setCategoryType(Category.INCOME);

                    mAmount.setTextColor(Color.parseColor("#00AA00"));
                    break;
                case R.id.expense_radio: // on expense we need category with type=expense
                    mBeneficiarRow.setVisibility(View.GONE);
                    mChargerRow.setVisibility(View.VISIBLE);

                    mCategoriesAdapter.setCategoryType(Category.EXPENSE);

                    mAmount.setTextColor(Color.parseColor("#AA0000"));
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.category_add_button: {
                final CategoryDialogFragment fragment = CategoryDialogFragment.newInstance(mCategoriesAdapter.getCategoryType());
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
        final DatabaseDAO db = DatabaseDAO.getInstance();
        try {
            if (mOperation != null) { // operation is already applied, need to revert it and re-apply again
                fillOperationFields();
                if (db.revertOperation(mOperation.getId())) { // revert original one and apply ours
                    dismiss();
                    // update operation fields in case accounts were affected when reverting
                    // ugly hack (lack of managed/non-managed entity difference)
                    if(mOperation.getCharger() != null)
                        mOperation.setCharger(Account.getFromDB(mOperation.getCharger().getId()));
                    if(mOperation.getBeneficiar() != null)
                        mOperation.setBeneficiar(Account.getFromDB(mOperation.getBeneficiar().getId()));
                    db.applyOperation(mOperation);
                }
                else
                    throw new IllegalStateException("Cannot reapply operation!"); // should never happen!!
            } else { // create new operation with data from fields specified
                mOperation = new Operation();
                fillOperationFields();
                if (db.applyOperation(mOperation)) // all succeeded
                    dismiss();
                else
                    throw new IllegalStateException("Cannot apply operation!"); // should never happen!!
            }
        } catch (IllegalArgumentException ex) {
            Toast.makeText(getWalletActivity(), ex.getMessage(), Toast.LENGTH_SHORT).show();
            mOperation = null;
        }
    }

    private void fillOperationFields() {
        // check data input
        final BigDecimal amount = getValue(mAmount.getText().toString(), BigDecimal.ZERO);
        if (amount.equals(BigDecimal.ZERO))
            throw new IllegalArgumentException(getString(R.string.operation_needs_amount));

        final String opCategoryID = ((UUIDCursorAdapter) mCategorySelector.getAdapter()).getItemUUID(mCategorySelector.getSelectedItemPosition());
        final Category opCategory = Category.getFromDB(opCategoryID);
        if (opCategory == null)
            throw new IllegalArgumentException(getString(R.string.operation_needs_category));
        // fill operation fields
        mOperation.setAmount(amount);
        mOperation.setCategory(opCategory);
        mOperation.setTime(mDatePicker.getCalendar().getTime());
        mOperation.setDescription(mDescription.getText().toString());
        switch (mTypeSwitch.getCheckedRadioButtonId()) { // prepare operation depending on type
            case R.id.transfer_radio: { // transfer op, we need 2 accounts
                final String chargerID = mAccountAdapter.getItemUUID(mChargeAccountSelector.getSelectedItemPosition());
                final String beneficiarID = mAccountAdapter.getItemUUID(mBeneficiarAccountSelector.getSelectedItemPosition());
                final Account chargeAcc = Account.getFromDB(chargerID);
                final Account benefAcc = Account.getFromDB(beneficiarID);
                if (chargeAcc == null || benefAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_transfer_accs));
                if (chargeAcc.getId().equals(benefAcc.getId())) // no transfer to self
                    throw new IllegalArgumentException(getString(R.string.accounts_identical));
                mOperation.setCharger(chargeAcc);
                mOperation.setBeneficiar(benefAcc);
                if (!mOperation.getBeneficiar().getCurrency().equals(mOperation.getCharger().getCurrency())) // different currencies
                    mOperation.setConvertingRate(getValue(mBeneficiarConversionRate.getText().toString(), 1d));
                break;
            }
            case R.id.income_radio: { // income op, we need beneficiar
                final String beneficiarID = mAccountAdapter.getItemUUID(mBeneficiarAccountSelector.getSelectedItemPosition());
                final Account benefAcc = Account.getFromDB(beneficiarID);
                if (benefAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_acc));
                mOperation.setBeneficiar(benefAcc);
                mOperation.setCharger(null);
                break;
            }
            case R.id.expense_radio: { // expense op, we need charger
                final String chargerID = mAccountAdapter.getItemUUID(mChargeAccountSelector.getSelectedItemPosition());
                final Account chargeAcc = Account.getFromDB(chargerID);
                if (chargeAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_acc));
                mOperation.setCharger(chargeAcc);
                mOperation.setBeneficiar(null);
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
        mCategoriesAdapter.changeCursor(null);
        mAccountAdapter.changeCursor(null);
        DatabaseDAO.getInstance().unregisterDatabaseListener(mCategoriesAdapter, DatabaseDAO.EntityType.CATEGORIES.toString());
    }
}
