package com.adonai.wallet.entities;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.adonai.wallet.R;

import java.sql.SQLException;

/**
 * Simple class prividing spinner views for all needs
 *
 * @author Adonai
 */
public class UUIDSpinnerAdapter<T extends Entity> extends UUIDCursorAdapter<T> implements SpinnerAdapter {

    public UUIDSpinnerAdapter(Activity context, Class<T> clazz) {
        super(context, clazz);
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
        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(resId, parent, false);
        } else
            view = convertView;

        try {
            mCursor.first();
            mCursor.moveRelative(position);
            int nameCol = mCursor.getRawResults().findColumn("name");
            String name = mCursor.getRawResults().getString(nameCol);

            final TextView nameText = (TextView) view.findViewById(android.R.id.text1);
            nameText.setText(name);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return view;
    }

    //abstract void fillView(T entity, View createdView);

}
