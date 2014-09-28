package com.adonai.wallet.entities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import com.adonai.wallet.R;
import com.adonai.wallet.database.EntityDao;

import java.sql.SQLException;

/**
 * Simple class prividing spinner views for all needs
 *
 * @author Adonai
 */
public abstract class UUIDSpinnerAdapter<T extends Entity> extends UUIDCursorAdapter<T> implements SpinnerAdapter {


    public UUIDSpinnerAdapter(Context context, EntityDao<T> entityDao) {
        super(context, entityDao);
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
            fillView(mCursor.moveRelative(position), view);

            //final TextView name = (TextView) view.findViewById(android.R.id.text1);
            //name.setText(mCursor.);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return view;
    }

    abstract void fillView(T entity, View createdView);

}
