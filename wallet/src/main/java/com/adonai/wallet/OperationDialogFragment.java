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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author adonai
 */
public class OperationDialogFragment extends WalletBaseDialogFragment implements View.OnClickListener {

    private EditText mDescription;
    private Button mCreateOperation;

    private final SimpleDateFormat mDateFormatter = new SimpleDateFormat("dd.MM.yyyy");
    private final Calendar mNow = Calendar.getInstance();
    private Button mDatePicker;

    private Spinner mChargeAccountSelector;
    private Spinner mCategorySelector;
    private AccountsAdapter mAccountAdapter;
    private EditText mAmountCharged;

    private RadioGroup mTypeSwitch;

    private final List<TableRow> conversionRows = new ArrayList<>();
    private TableRow mCategoryRow;
    private TableRow mBeneficiarRow;

    private Spinner mBeneficiarAccountSelector;
    private EditText mBeneficiarConversionRate;
    private TextView mBeneficiarAmountDelivered;

    private CategoriesAdapter mInCategoriesAdapter;
    private CategoriesAdapter mOutCategoriesAdapter;

    private Operation mOperation;

    public OperationDialogFragment() {
        //super();
    }

    public OperationDialogFragment(Operation toModify) {
        super();

        if(toModify != null)
            mOperation = toModify;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAccountAdapter = new AccountsAdapter(getWalletActivity());
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

        mInCategoriesAdapter = new CategoriesAdapter(getWalletActivity(), Category.INCOME);
        mOutCategoriesAdapter = new CategoriesAdapter(getWalletActivity(), Category.EXPENSE);
        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mCategorySelector.setOnItemSelectedListener(new CategorySelectListener());

        mAmountCharged = (EditText) dialog.findViewById(R.id.charge_amount_edit);
        mAmountCharged.addTextChangedListener(textWatcher);

        mTypeSwitch = (RadioGroup) dialog.findViewById(R.id.operation_type_switch);
        mTypeSwitch.setOnCheckedChangeListener(new TypeSelector());

        mBeneficiarRow = (TableRow) dialog.findViewById(R.id.beneficiar_row);
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

        } else {
            mTypeSwitch.check(R.id.expense_radio);
            mDatePicker.setText(mDateFormatter.format(mNow.getTime())); // current time
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
                if(position != -1)
                    mChargeAccountSelector.setSelection(accPosition);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class AccountSelectListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Account chargeAcc = getWalletActivity().getEntityDAO().getAccount(mChargeAccountSelector.getSelectedItemId());
            Account beneficiarAcc = getWalletActivity().getEntityDAO().getAccount(mBeneficiarAccountSelector.getSelectedItemId());
            if(!chargeAcc.getCurrency().equals(beneficiarAcc.getCurrency())) { // show conversion rows when curencies are different
                for(TableRow row : conversionRows)
                    row.setVisibility(View.VISIBLE);

                updateConversionAmount();
            } else {
                for(TableRow row : conversionRows)
                    row.setVisibility(View.GONE);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public void updateConversionAmount() {
        final BigDecimal amount = Utils.getValue(mAmountCharged.getText().toString(), BigDecimal.ZERO);
        final Double rate = Utils.getValue(mBeneficiarConversionRate.getText().toString(), 1d);
        final BigDecimal beneficiarAmount = amount.divide(BigDecimal.valueOf(rate));
        mBeneficiarAmountDelivered.setText(beneficiarAmount.toPlainString());
    }

    private class AccountsAdapter extends CursorAdapter implements SpinnerAdapter {
        public AccountsAdapter(Context context) {
            super(context, getWalletActivity().getEntityDAO().getAccountCursor(), false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(android.R.layout.simple_spinner_item, viewGroup, false);
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView name = (TextView) view.findViewById(android.R.id.text1);
            name.setText(cursor.getString(1));
        }

        public int getPosition(long id) {
            if(getItemId((int) id) == id)
                return (int) id;

            Cursor catCur = getCursor();
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

        public CategoriesAdapter(Context context, int categoryType) {
            super(context, getWalletActivity().getEntityDAO().getCategoryCursor(categoryType), false);
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
    }

    private class TypeSelector implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.transfer_radio: // on transfer we don't need category but have to specify beneficiar account
                    mBeneficiarRow.setVisibility(View.VISIBLE);
                    mCategoryRow.setVisibility(View.GONE);
                    mAmountCharged.setTextColor(Color.parseColor("#0000AA"));
                    break;
                case R.id.income_radio: // on income we need category with type=income
                    mBeneficiarRow.setVisibility(View.GONE);

                    mCategoryRow.setVisibility(View.VISIBLE);
                    mCategorySelector.setAdapter(mInCategoriesAdapter);

                    mAmountCharged.setTextColor(Color.parseColor("#00AA00"));
                    break;
                case R.id.expense_radio: // on expense we need category with type=expense
                    mBeneficiarRow.setVisibility(View.GONE);

                    mCategoryRow.setVisibility(View.VISIBLE);
                    mCategorySelector.setAdapter(mOutCategoriesAdapter);

                    mAmountCharged.setTextColor(Color.parseColor("#AA0000"));
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_modify_operation_button: {

                if(mOperation != null)  {

                } else try { // create new operation with data from fields specified
                    final DatabaseDAO db = getWalletActivity().getEntityDAO();

                    // check fields!
                    final Account chargeAcc = db.getAccount(mChargeAccountSelector.getSelectedItemId());
                    if(chargeAcc == null)
                        throw new IllegalArgumentException(getString(R.string.operation_needs_acc));

                    final BigDecimal chargeAmount = Utils.getValue(mAmountCharged.getText().toString(), BigDecimal.ZERO);
                    if(chargeAmount.equals(BigDecimal.ZERO))
                        throw new IllegalArgumentException(getString(R.string.operation_needs_amount));

                    final Category opCategory = db.getCategory(mCategorySelector.getSelectedItemId());
                    if(opCategory == null)
                        throw new IllegalArgumentException(getString(R.string.operation_needs_category));

                    final Operation operation = new Operation(chargeAcc, chargeAmount, opCategory);
                    operation.setTime(mNow.getTime());
                    operation.setDescription(mDescription.getText().toString());
                    if(mTypeSwitch.getCheckedRadioButtonId() == R.id.transfer_radio) { // if this is a transfer op
                        if(mBeneficiarAccountSelector.getSelectedItemId() == mChargeAccountSelector.getSelectedItemId()) // no transfer to self
                            throw new IllegalArgumentException(getString(R.string.accounts_identical));

                        operation.setBeneficiar(db.getAccount(mBeneficiarAccountSelector.getSelectedItemId()));
                        if(!operation.getBeneficiar().getCurrency().equals(operation.getCharger().getCurrency())) // different currencies
                            operation.setConvertingRate(Utils.getValue(mBeneficiarConversionRate.getText().toString(), 1d));

                    }
                    if(db.addOperation(operation) != -1 && db.applyOperation(operation)) // all succeeded
                        dismiss();
                    else
                        Toast.makeText(getWalletActivity(), getString(R.string.cannot_apply_op), Toast.LENGTH_SHORT).show();

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
                        mDatePicker.setText(mDateFormatter.format(mNow.getTime()));
                    }
                };
                new DatePickerDialog(getActivity(), listener, mNow.get(Calendar.YEAR), mNow.get(Calendar.MONTH), mNow.get(Calendar.DAY_OF_MONTH)).show();
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
            if(Utils.getValue(mBeneficiarConversionRate.getText().toString(), 1d) == 0d) // do not divide by zero
                mBeneficiarConversionRate.setText("1");

            updateConversionAmount();
        }
    }
}
