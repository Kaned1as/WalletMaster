package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
import android.widget.Toast;

import com.adonai.wallet.entities.UUIDSpinnerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.WindowManager.LayoutParams;

/**
 * Created by adonai on 12.06.14.
 */
public class WalletBaseFilterFragment extends WalletBaseDialogFragment implements View.OnClickListener {

    public enum FilterType {
        AMOUNT,
        TEXT,
        DATE,
        FOREIGN_ID
    }

     public interface FilterCursorListener {
         void OnFilterCompleted(Cursor cursor);
         void resetFilter();
     }

    public void setFilterCursorListener(FilterCursorListener listener) {
        this.listener = listener;
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
    private FilterCursorListener listener;

    private ArrayAdapter<String> mTypeAdapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(mAllowedToFilter == null)
            dismiss();

        mTypeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mAllowedToFilter.keySet().toArray(new String[mAllowedToFilter.keySet().size()]));

        final View dialog = getActivity().getLayoutInflater().inflate(R.layout.entity_filter_dialog, null);
        assert dialog != null;
        mFiltersRoot = (LinearLayout) dialog.findViewById(R.id.filters_layout);
        mAddFilterButton = (ImageButton) dialog.findViewById(R.id.add_filter_button);
        mAddFilterButton.setOnClickListener(this);
        addRow();


        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(listener != null)
                    listener.resetFilter();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(listener != null)
                    try {
                        listener.OnFilterCompleted(getFilterCursor());
                    } catch (FilterFormatException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            }
        });

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().clearFlags(LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for(final Pair<FilterType, Object> values : mAllowedToFilter.values())
            if(values.first == FilterType.FOREIGN_ID)
                ((Cursor)values.second).close();
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

        typeSelector.setAdapter(mTypeAdapter);
        typeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                while (filterLayout.getChildCount() > 1) // remove old views
                    filterLayout.removeViewAt(1);

                final Pair<FilterType, Object> filterType = mAllowedToFilter.get(mTypeAdapter.getItem(position));
                switch (filterType.first) {
                    case AMOUNT: {
                        final Spinner signSelector = new Spinner(getActivity());
                        signSelector.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{">", "=", "<"}));
                        final EditText numberInput = new EditText(getActivity());
                        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
                        final EditText textInput = new EditText(getActivity());
                        textInput.setInputType(InputType.TYPE_CLASS_TEXT);
                        filterLayout.addView(equalSign);
                        filterLayout.addView(textInput);
                        equalSign.setLayoutParams(forSigns);
                        textInput.setLayoutParams(forSelectors);
                        textInput.setTag(filterType.second);
                        break;
                    }
                    case DATE: {
                        final Spinner signSelector = new Spinner(getActivity());
                        signSelector.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{">", "<"}));
                        final EditText mDatePicker = new EditText(getActivity());
                        mDatePicker.setGravity(Gravity.CENTER);
                        filterLayout.addView(signSelector);
                        filterLayout.addView(mDatePicker);
                        signSelector.setLayoutParams(forSigns);
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

    public Cursor getFilterCursor() throws FilterFormatException {
        final StringBuilder sb = new StringBuilder(100);
        sb.append("SELECT * FROM ");
        sb.append(mTableName);

        final List<String> args = new ArrayList<>();
        boolean firstPassed = false;
        for(int i = 0; i < mFiltersRoot.getChildCount(); ++i) {
            final String toAdd;
            final LinearLayout filterLayout = (LinearLayout) mFiltersRoot.getChildAt(i);
            final Spinner typeSelector = (Spinner) filterLayout.getChildAt(0);
            final Pair<FilterType, Object> filter = mAllowedToFilter.get(typeSelector.getSelectedItem().toString());
            switch (filter.first) {
                case AMOUNT: {
                    final Spinner signSelector = (Spinner) filterLayout.getChildAt(1);
                    final EditText amountInput = (EditText) filterLayout.getChildAt(2);
                    final String sign = signSelector.getSelectedItem().toString();
                    final String column = (String) filter.second;
                    final String amount = amountInput.getText().toString();
                    if (!amount.isEmpty()) {
                        toAdd = column + " " + sign + " ?";
                        args.add(amount);
                    } else
                        toAdd = null;
                    break;
                }
                case TEXT: {
                    final EditText textInput = (EditText) filterLayout.getChildAt(2);
                    final String column = (String) filter.second;
                    final String text = textInput.getText().toString();
                    if (!text.isEmpty()) {
                        toAdd = column + " = ?";
                        args.add(text);
                    } else
                        toAdd = null;
                    break;
                }
                case DATE: {
                    final Spinner signSelector = (Spinner) filterLayout.getChildAt(1);
                    final EditText timeInput = (EditText) filterLayout.getChildAt(2);
                    final DatePickerListener dateHolder = (DatePickerListener) timeInput.getOnFocusChangeListener();
                    final String sign = signSelector.getSelectedItem().toString();
                    final String column = (String) filter.second;
                    if(timeInput.getText().length() > 0) {
                        toAdd = column + " " + sign + " ?";
                        args.add(String.valueOf(dateHolder.getCalendar().getTimeInMillis()));
                    } else
                        toAdd = null;
                    break;
                }
                case FOREIGN_ID: {
                    final Spinner idSelector = (Spinner) filterLayout.getChildAt(2);
                    UUIDSpinnerAdapter cursorHolder = (UUIDSpinnerAdapter) idSelector.getAdapter();
                    String uuid = cursorHolder.getItemUUID(idSelector.getSelectedItemPosition());
                    final String column = ((Cursor) filter.second).getColumnName(0);
                    toAdd = column + " = ?";
                    args.add(uuid);
                    break;
                }
                default:
                    toAdd = null;
                    break;

            }
            if(toAdd != null)
                if (firstPassed) {
                    sb.append(" AND ");
                    sb.append(toAdd);
                } else {
                    sb.append(" WHERE ");
                    sb.append(toAdd);
                    firstPassed = true;
                }
        }
        if(!firstPassed) // we did at least one iteration
            throw new FilterFormatException(getString(R.string.no_filter_specified));

        return DatabaseDAO.getInstance().select(sb.toString(), args.toArray(new String[args.size()]));
    }

    public static class FilterFormatException extends Exception {
        public FilterFormatException(String detailMessage) {
            super(detailMessage);
        }
    }
}
