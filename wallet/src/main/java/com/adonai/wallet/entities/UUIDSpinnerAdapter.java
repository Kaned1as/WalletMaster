package com.adonai.wallet.entities;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.adonai.wallet.R;

/**
 * Simple class prividing spinner views for all needs
 *
 * @author Adonai
 */
public class UUIDSpinnerAdapter extends UUIDCursorAdapter implements SpinnerAdapter {

    public UUIDSpinnerAdapter(Context context, Cursor cursor) {
        super(context, cursor);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return newView(position, convertView, parent, R.layout.tall_list_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return newView(position, convertView, parent, android.R.layout.simple_spinner_item);
    }

    public View newView(int position, View convertView, ViewGroup parent, int resId) {
        final View view;
        mCursor.moveToPosition(position);

        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(resId, parent, false);
        } else
            view = convertView;

        final TextView name = (TextView) view.findViewById(android.R.id.text1);
        name.setText(mCursor.getString(1));

        return view;
    }

}
