package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.adonai.wallet.entities.UUIDSpinnerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by adonai on 12.06.14.
 */
public class WalletBaseFilterFragment extends WalletBaseDialogFragment implements View.OnClickListener {

    private static final String FILTER_REFERENCE = "filter.map";

    public enum FilterType {
        AMOUNT,
        TEXT,
        DATE,
        FOREIGN_ID
    }

    public static WalletBaseFilterFragment newInstance(String tableName, Map<String, Pair<FilterType, Object>> allowedToFilter) {
        final WalletBaseFilterFragment fragment = new WalletBaseFilterFragment();
        fragment.mAllowedToFilter = allowedToFilter;
        fragment.mTableName = tableName;
        return fragment;
    }

                   /* caption,     filter    , column    */
    private Map<String, Pair<FilterType, Object>> mAllowedToFilter = new HashMap<>(10);
    private String mTableName;
    private LinearLayout mFiltersRoot;
    private ImageButton mAddFilterButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(mAllowedToFilter == null)
            dismiss();

        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.entity_filter_dialog, null);
        assert dialog != null;
        mFiltersRoot = (LinearLayout) dialog.findViewById(R.id.filters_layout);
        mAddFilterButton = (ImageButton) dialog.findViewById(R.id.add_filter_button);
        mAddFilterButton.setOnClickListener(this);
        addRow();


        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, null);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_filter_button:
                addRow();
                break;
        }
    }

    public void addRow() {
        // main layout
        final LinearLayout filterLayout = new LinearLayout(getActivity());
        filterLayout.setOrientation(LinearLayout.HORIZONTAL);

        // spinner to select filter type
        final LinearLayout.LayoutParams forSelectors = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        final LinearLayout.LayoutParams forSigns = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
        final Spinner typeSelector = new Spinner(getActivity());
        typeSelector.setLayoutParams(forSelectors);

        final ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mAllowedToFilter.keySet().toArray(new String[mAllowedToFilter.keySet().size()]));
        typeSelector.setAdapter(typeAdapter);
        typeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                while (filterLayout.getChildCount() > 1) // remove old views
                    filterLayout.removeViewAt(1);

                final Pair<FilterType, Object> filterType = mAllowedToFilter.get(typeAdapter.getItem(position));
                switch (filterType.first) {
                    case AMOUNT: {
                        final Spinner signSelector = new Spinner(getActivity());
                        signSelector.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{">", "=", "<"}));
                        final EditText numberInput = new EditText(getActivity());
                        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                        filterLayout.addView(signSelector);
                        filterLayout.addView(numberInput);
                        signSelector.setLayoutParams(forSigns);
                        numberInput.setLayoutParams(forSelectors);
                        numberInput.setTag(filterType.second);
                        break;
                    }
                    case TEXT: {
                        final TextView equalSign = new TextView(getActivity());
                        equalSign.setText("=");
                        equalSign.setGravity(Gravity.CENTER);
                        final EditText numberInput = new EditText(getActivity());
                        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                        filterLayout.addView(equalSign);
                        filterLayout.addView(numberInput);
                        equalSign.setLayoutParams(forSigns);
                        numberInput.setLayoutParams(forSelectors);
                        numberInput.setTag(filterType.second);
                        break;
                    }
                    case DATE: {
                        final TextView equalSign = new TextView(getActivity());
                        equalSign.setText("=");
                        equalSign.setGravity(Gravity.CENTER);
                        final EditText mDatePicker = new EditText(getActivity());
                        mDatePicker.setGravity(Gravity.CENTER);
                        filterLayout.addView(equalSign);
                        filterLayout.addView(mDatePicker);
                        equalSign.setLayoutParams(forSigns);
                        mDatePicker.setLayoutParams(forSelectors);
                        final DatePickerListener dialog = new DatePickerListener(mDatePicker);
                        mDatePicker.setOnFocusChangeListener(dialog);
                        mDatePicker.setOnClickListener(dialog);
                        mDatePicker.setTag(filterType.second);
                        break;
                    }
                    case FOREIGN_ID: {
                        final TextView equalSign = new TextView(getActivity());
                        equalSign.setText("=");
                        equalSign.setGravity(Gravity.CENTER);
                        final Spinner entitySelector = new Spinner(getActivity());
                        entitySelector.setAdapter(new UUIDSpinnerAdapter(getActivity(), (android.database.Cursor) filterType.second));
                        filterLayout.addView(equalSign);
                        filterLayout.addView(entitySelector);
                        equalSign.setLayoutParams(forSigns);
                        entitySelector.setLayoutParams(forSelectors);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        filterLayout.addView(typeSelector);
        mFiltersRoot.addView(filterLayout);
    }
}
