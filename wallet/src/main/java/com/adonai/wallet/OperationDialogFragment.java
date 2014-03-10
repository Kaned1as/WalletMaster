package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableRow;
import android.widget.TextView;

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

    private final List<TableRow> beneficiarRows = new ArrayList<>();
    private Spinner mBeneficiarAccountSelector;
    private EditText mBeneficiarConversionRate;
    private TextView mBeneficiarAmountDelivered;

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
        mAccountAdapter = new AccountsAdapter(getWalletActivity(), false);

        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.operation_create_modify_dialog, null);
        assert dialog != null;

        mDescription = (EditText) dialog.findViewById(R.id.description_edit);
        mCreateOperation = (Button) dialog.findViewById(R.id.create_modify_operation_button);
        mCreateOperation.setOnClickListener(this);
        mDatePicker = (Button) dialog.findViewById(R.id.date_picker_button);
        mDatePicker.setOnClickListener(this);

        mChargeAccountSelector = (Spinner) dialog.findViewById(R.id.charge_account_spinner);
        mChargeAccountSelector.setAdapter(mAccountAdapter);

        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mAmountCharged = (EditText) dialog.findViewById(R.id.charge_amount_edit);

        mTypeSwitch = (RadioGroup) dialog.findViewById(R.id.operation_type_switch);
        mTypeSwitch.setOnCheckedChangeListener(new TypeSelector());

        beneficiarRows.add((TableRow) dialog.findViewById(R.id.beneficiar_layout));
        beneficiarRows.add((TableRow) dialog.findViewById(R.id.beneficiar_conversion));
        beneficiarRows.add((TableRow) dialog.findViewById(R.id.beneficiar_amount));

        mBeneficiarAccountSelector = (Spinner) dialog.findViewById(R.id.beneficiar_account_spinner);
        mBeneficiarAccountSelector.setAdapter(mAccountAdapter);

        mBeneficiarConversionRate = (EditText) dialog.findViewById(R.id.beneficiar_conversion_edit);
        mBeneficiarAmountDelivered = (TextView) dialog.findViewById(R.id.beneficiar_amount_value_label);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);

        // if we are modifying existing operation
        if(mOperation != null) {

        } else {
            mDatePicker.setText(mDateFormatter.format(mNow.getTime())); // current time
        }

        return builder.create();
    }

    private class AccountsAdapter extends CursorAdapter implements SpinnerAdapter {
        public AccountsAdapter(Context context, boolean autoRequery) {
            super(context, getWalletActivity().getEntityDAO().getAcountCursor(), autoRequery);
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

        @Override
        public long getItemId(int position) {
            getCursor().moveToPosition(position);

            return getCursor().getLong(0);
        }
    }

    private class TypeSelector implements RadioGroup.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.transfer_radio:
                    for(TableRow row : beneficiarRows)
                        row.setVisibility(View.VISIBLE);
                    break;
                case R.id.income_radio:
                    for(TableRow row : beneficiarRows)
                        row.setVisibility(View.GONE);
                    break;
                case R.id.outcome_radio:
                    for(TableRow row : beneficiarRows)
                        row.setVisibility(View.GONE);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_modify_operation_button: {

                if(mOperation != null)  {

                } else {
                    Operation operation = new Operation();
                    operation.setDescription(mDescription.getText().toString());
                    operation.setAmountCharged(Utils.getValue(mAmountCharged.getText().toString(), BigDecimal.ZERO));
                    operation.setTime(mNow.getTime());
                    operation.setCategory(null); // TODO: FIX
                    dismiss();
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
}
