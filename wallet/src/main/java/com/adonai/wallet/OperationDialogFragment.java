package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Currency;
import com.adonai.wallet.entities.Operation;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by adonai on 09.03.14.
 */
public class OperationDialogFragment extends DialogFragment implements View.OnClickListener {

    private EditText mDescription;
    private Button mCreateOperation;

    private final SimpleDateFormat mDateFormatter = new SimpleDateFormat("dd.MM.yyyy");
    private final Calendar mNow = Calendar.getInstance();
    private Button mDatePicker;

    private Spinner mChargeAccountSelector;
    private Spinner mCategorySelector;
    private EditText mAmountCharged;

    private CheckBox mTransferSwitch;

    private Spinner mBeneficiarAccountSelector;
    private EditText mBeneficiarConversionRate;
    private TextView mAmountDelivered;

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
        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.operation_create_modify_dialog, null);
        assert dialog != null;

        mDescription = (EditText) dialog.findViewById(R.id.description_edit);
        mCreateOperation = (Button) dialog.findViewById(R.id.create_modify_operation_button);
        mCreateOperation.setOnClickListener(this);
        mDatePicker = (Button) dialog.findViewById(R.id.date_picker_button);
        mDatePicker.setOnClickListener(this);

        mChargeAccountSelector = (Spinner) dialog.findViewById(R.id.charge_account_spinner);
        mCategorySelector = (Spinner) dialog.findViewById(R.id.category_spinner);
        mAmountCharged = (EditText) dialog.findViewById(R.id.charge_amount_edit);

        mTransferSwitch = (CheckBox) dialog.findViewById(R.id.transfer_switch);
        mBeneficiarAccountSelector = (Spinner) dialog.findViewById(R.id.beneficiar_account_spinner);
        mBeneficiarConversionRate = (EditText) dialog.findViewById(R.id.beneficiar_conversion_edit);
        mAmountDelivered = (TextView) dialog.findViewById(R.id.beneficiar_amount_value_label);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);

        // if we are modifying existing operation
        if(mOperation != null) {

        } else {
            mDatePicker.setText(mDateFormatter.format(mNow.getTime())); // current time
        }

        return builder.create();
    }

    public class AccountsAdapter extends CursorAdapter<Account> implements SpinnerAdapter {

        public CurrencyAdapter(Context context, int resource, List<Currency> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view;
            Currency currency = getItem(position);
            if (convertView == null)
                view = View.inflate(getContext(), R.layout.currency_list_item, null);
            else
                view = convertView;

            TextView title = (TextView) view.findViewById(R.id.curr_caption_text);
            title.setText(currency.getCode());
            TextView author = (TextView) view.findViewById(R.id.curr_description_text);
            author.setText(currency.getDescription());
            TextView last_post = (TextView) view.findViewById(R.id.curr_usedin_text);
            last_post.setText(currency.getUsedIn());

            return view;
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
}
