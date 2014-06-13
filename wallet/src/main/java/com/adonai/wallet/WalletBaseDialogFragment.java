package com.adonai.wallet;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;

import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;


/**
 * All wallet master dialog fragments must extend this class
 *
 * @author adonai
 */
public class WalletBaseDialogFragment extends DialogFragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }

    protected class DatePickerListener implements View.OnClickListener, View.OnFocusChangeListener {

        private Calendar mSelectedTime = Calendar.getInstance();
        private final EditText mText;

        public DatePickerListener(EditText text) {
            mText = text;
        }

        @Override
        public void onClick(View v) {
            handleClick();
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(!hasFocus)
                return;

            handleClick();
        }

        private void handleClick() {
            DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    mSelectedTime.set(year, monthOfYear, dayOfMonth);
                    mText.setText(VIEW_DATE_FORMAT.format(mSelectedTime.getTime()));
                }
            };
            new DatePickerDialog(getActivity(), listener, mSelectedTime.get(Calendar.YEAR), mSelectedTime.get(Calendar.MONTH), mSelectedTime.get(Calendar.DAY_OF_MONTH)).show();
        }

        public Calendar getCalendar() {
            return mSelectedTime;
        }

        public void setCalendar(Date mNow) {
            this.mSelectedTime.setTime(mNow);
        }
    }
}
