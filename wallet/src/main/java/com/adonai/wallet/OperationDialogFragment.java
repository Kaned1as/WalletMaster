package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Operation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.adonai.wallet.Utils.*;

/**
 * @author adonai
 */
public class OperationDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {

    private EditText mDescription;
    private Button mCreateOperation;

    private final Calendar mNow = Calendar.getInstance();
    private Button mDatePicker;

    private Spinner mChargeAccountSelector;
    private Spinner mCategorySelector;
    private AccountsAdapter mAccountAdapter;
    private EditText mAmount;

    private RadioGroup mTypeSwitch;

    private final List<TableRow> conversionRows = new ArrayList<>();
    private TableRow mCategoryRow;
    private TableRow mBeneficiarRow;
    private TableRow mChargerRow;

    private Spinner mBeneficiarAccountSelector;
    private EditText mBeneficiarConversionRate;
    private TextView mBeneficiarAmountDelivered;

    private CategoriesAdapter mInCategoriesAdapter;
    private CategoriesAdapter mOutCategoriesAdapter;

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
        mAccountAdapter = new AccountsAdapter();
        final AccountSelectListener accountSelectListener = new AccountSelectListener();
        final CountChangedWatcher textWatcher = new CountChangedWatcher();

        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.operation_create_modify_dialog, null);
        assert dialog != null;

        mDescription = (EditText) dialog.findViewById(R.id.description_edit);
        mCreateOperation = (Button) dialog.findViewById(R.id.create_modify_operation_button);
        mCreateOperation.setOnClickListener(this);
        mDatePicker = (Button) dialog.findViewById(R.id.date_picker_button);
        mDatePicker.setOnClickListener(this);

        mChargeAccountSelector = (Spinner) dialog.findViewById(R.id.charge_account_spinner);
        mChargeAccountSelector.setAdapter(mAccountAdapter);
        mChargeAccountSelector.setOnItemSelectedListener(accountSelectListener);

        mInCategoriesAdapter = new CategoriesAdapter(Category.INCOME);
        mOutCategoriesAdapter = new CategoriesAdapter(Category.EXPENSE);
        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mCategorySelector.setOnItemSelectedListener(new CategorySelectListener());

        mAmount = (EditText) dialog.findViewById(R.id.amount_edit);
        mAmount.addTextChangedListener(textWatcher);

        mTypeSwitch = (RadioGroup) dialog.findViewById(R.id.operation_type_switch);
        mTypeSwitch.setOnCheckedChangeListener(new TypeSelector());

        mChargerRow = (TableRow) dialog.findViewById(R.id.operation_charge_row);
        mBeneficiarRow = (TableRow) dialog.findViewById(R.id.operation_beneficiar_row);
        conversionRows.add((TableRow) dialog.findViewById(R.id.beneficiar_conversion));
        conversionRows.add((TableRow) dialog.findViewById(R.id.beneficiar_amount));
        mCategoryRow = (TableRow) dialog.findViewById(R.id.operation_category_row);

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
            mDescription.setText(mOperation.getDescription());
            mAmount.setText(mOperation.getAmount().toPlainString());
            mNow.setTime(mOperation.getTime().getTime());
            mDatePicker.setText(VIEW_DATE_FORMAT.format(mNow.getTime()));
            switch (mOperation.getOperationType()) {
                case TRANSFER:
                    mTypeSwitch.check(R.id.transfer_radio);
                    mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getCharger().getId()));
                    mBeneficiarAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getBeneficiar().getId()));
                    if(mOperation.getConvertingRate() != null)
                        mBeneficiarConversionRate.setText(mOperation.getConvertingRate().toString());
                    break;
                case EXPENSE:
                    mTypeSwitch.check(R.id.expense_radio);
                    mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getCharger().getId()));
                    mCategorySelector.setSelection(mOutCategoriesAdapter.getPosition(mOperation.getCategory().getId()));
                    break;
                case INCOME:
                    mTypeSwitch.check(R.id.income_radio);
                    mBeneficiarAccountSelector.setSelection(mAccountAdapter.getPosition(mOperation.getBeneficiar().getId()));
                    mCategorySelector.setSelection(mInCategoriesAdapter.getPosition(mOperation.getCategory().getId()));
                    break;
            }
        } else { // this is new operation
            mTypeSwitch.check(R.id.expense_radio);
            mDatePicker.setText(VIEW_DATE_FORMAT.format(mNow.getTime())); // current time
            if(mCharger != null)
                mChargeAccountSelector.setSelection(mAccountAdapter.getPosition(mCharger.getId()));
        }

        return builder.create();
    }

    private class CategorySelectListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final Category cat = getWalletActivity().getEntityDAO().getCategory(id);
            if (cat.getPreferredAccount() != null)  { // selected category has preferred account
                final long accId = cat.getPreferredAccount().getId();
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
        Account chargeAcc = getWalletActivity().getEntityDAO().getAccount(mChargeAccountSelector.getSelectedItemId());
        Account beneficiarAcc = getWalletActivity().getEntityDAO().getAccount(mBeneficiarAccountSelector.getSelectedItemId());
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

    private class AccountsAdapter extends CursorAdapter implements SpinnerAdapter {
        public AccountsAdapter() {
            super(getActivity(), getWalletActivity().getEntityDAO().getAccountCursor(), false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(android.R.layout.simple_spinner_item, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView name = (TextView) view.findViewById(android.R.id.text1);
            name.setText(cursor.getString(1));
        }

        public int getPosition(long id) {
            if(getItemId((int) id) == id)
                return (int) id;

            final Cursor catCur = getCursor();
            assert catCur != null;

            catCur.moveToFirst();
            do {
                if(catCur.getLong(0) == id)
                    return catCur.getPosition();
            } while(catCur.moveToNext());

            return -1;
        }
    }

    private class CategoriesAdapter extends CursorAdapter implements SpinnerAdapter, DatabaseDAO.DatabaseListener {
        private final int mCategoryType;

        public CategoriesAdapter(int categoryType) {
            super(getActivity(), getWalletActivity().getEntityDAO().getCategoryCursor(categoryType), false);
            mCategoryType = categoryType;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(android.R.layout.simple_spinner_item, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView name = (TextView) view.findViewById(android.R.id.text1);
            name.setText(cursor.getString(1));
        }

        @Override
        public void handleUpdate() {
            changeCursor(getWalletActivity().getEntityDAO().getCategoryCursor(mCategoryType));
        }

        public int getPosition(long id) {
            if(getItemId((int) id) == id)
                return (int) id;

            final Cursor catCur = getCursor();
            assert catCur != null;

            catCur.moveToFirst();
            do {
                if(catCur.getLong(0) == id)
                    return catCur.getPosition();
            } while(catCur.moveToNext());

            return -1;
        }
    }

    private class TypeSelector implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.transfer_radio: // on transfer we don't need category but have to specify beneficiar account
                    mBeneficiarRow.setVisibility(View.VISIBLE);
                    mChargerRow.setVisibility(View.VISIBLE);

                    mCategoryRow.setVisibility(View.GONE);
                    mAmount.setTextColor(Color.parseColor("#0000AA"));

                    updateTransferConversionVisibility();
                    break;
                case R.id.income_radio: // on income we need category with type=income
                    mBeneficiarRow.setVisibility(View.VISIBLE);
                    mChargerRow.setVisibility(View.GONE);

                    mCategoryRow.setVisibility(View.VISIBLE);
                    mCategorySelector.setAdapter(mInCategoriesAdapter);

                    mAmount.setTextColor(Color.parseColor("#00AA00"));
                    break;
                case R.id.expense_radio: // on expense we need category with type=expense
                    mBeneficiarRow.setVisibility(View.GONE);
                    mChargerRow.setVisibility(View.VISIBLE);

                    mCategoryRow.setVisibility(View.VISIBLE);
                    mCategorySelector.setAdapter(mOutCategoriesAdapter);

                    mAmount.setTextColor(Color.parseColor("#AA0000"));
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_modify_operation_button: {
                final DatabaseDAO db = getWalletActivity().getEntityDAO();
                try {
                    if (mOperation != null) { // operation is already applied, need to revert it and re-apply again
                        fillOperationFields(db);
                        if (db.revertOperation(mOperation.getId()) && db.applyOperation(mOperation)) // revert original one and apply ours
                            dismiss();
                         else
                            throw new IllegalStateException("Cannot reapply operation!"); // should never happen!!
                    } else { // create new operation with data from fields specified
                        mOperation = new Operation();
                        fillOperationFields(db);
                        if (db.applyOperation(mOperation)) // all succeeded
                            dismiss();
                        else
                            throw new IllegalStateException("Cannot apply operation!"); // should never happen!!
                    }
                } catch (IllegalArgumentException ex) {
                    Toast.makeText(getWalletActivity(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.date_picker_button: {
                DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        mNow.set(year, monthOfYear, dayOfMonth);
                        mDatePicker.setText(VIEW_DATE_FORMAT.format(mNow.getTime()));
                    }
                };
                new DatePickerDialog(getActivity(), listener, mNow.get(Calendar.YEAR), mNow.get(Calendar.MONTH), mNow.get(Calendar.DAY_OF_MONTH)).show();
            }
        }
    }

    private void fillOperationFields(DatabaseDAO db) {
        // check data input
        final BigDecimal amount = getValue(mAmount.getText().toString(), BigDecimal.ZERO);
        if (amount.equals(BigDecimal.ZERO))
            throw new IllegalArgumentException(getString(R.string.operation_needs_amount));

        final Category opCategory = db.getCategory(mCategorySelector.getSelectedItemId());
        if (opCategory == null)
            throw new IllegalArgumentException(getString(R.string.operation_needs_category));
        // fill operation fields
        mOperation.setAmount(amount);
        mOperation.setCategory(opCategory);
        mOperation.setTime(mNow.getTime());
        mOperation.setDescription(mDescription.getText().toString());
        switch (mTypeSwitch.getCheckedRadioButtonId()) { // prepare operation depending on type
            case R.id.transfer_radio: { // transfer op, we need 2 accounts
                final Account chargeAcc = db.getAccount(mChargeAccountSelector.getSelectedItemId());
                final Account benefAcc = db.getAccount(mBeneficiarAccountSelector.getSelectedItemId());
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
                final Account benefAcc = db.getAccount(mBeneficiarAccountSelector.getSelectedItemId());
                if (benefAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_acc));
                mOperation.setBeneficiar(benefAcc);
                mOperation.setCharger(null);
                break;
            }
            case R.id.expense_radio: { // expense op, we need charger
                final Account chargeAcc = db.getAccount(mChargeAccountSelector.getSelectedItemId());
                if (chargeAcc == null)
                    throw new IllegalArgumentException(getString(R.string.operation_needs_acc));
                mOperation.setCharger(chargeAcc);
                mOperation.setBeneficiar(null);
                break;
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mAccountAdapter.changeCursor(null); // close old cursor
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
}
