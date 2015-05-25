package com.adonai.wallet.adapters;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.adonai.wallet.WalletBaseDialogFragment;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Entity;
import com.j256.ormlite.android.AndroidDatabaseResults;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by adonai on 26.05.15.
 */
public class WithDefaultAdapter<T extends Entity> extends UUIDSpinnerAdapter<T> {
    private final int mNoneResId;

    public WithDefaultAdapter(Fragment fragment, Class<T> clazz, int defaultTextResId) {
        super(fragment.getActivity(), clazz);
        this.mNoneResId = defaultTextResId;
    }

    @Override
    public View newView(int position, View convertView, ViewGroup parent, int resId) {
        final View view;

        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(resId, parent, false);
        } else
            view = convertView;

        final TextView name = (TextView) view.findViewById(android.R.id.text1);
        if (position == 0)
            name.setText(mNoneResId);
        else {
            try {
                ((AndroidDatabaseResults) mCursor.getRawResults()).moveAbsolute(position - 1);
                T entity = mCursor.current();
                name.setText(entity.toString());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return view;
    }
    
    @Override
    public int getCount() {
        return super.getCount() + 1;
    }

    @Override
    public T getItem(int position) {
        if (position == 0)
            return null;
        else
            return super.getItem(position - 1);
    }

    @Override
    public long getItemId(int position) {
        if (position == 0)
            return -1;
        else
            return super.getItemId(position - 1);
    }

    @Override
    public UUID getItemUUID(int position) {
        if (position == 0)
            return null;
        else
            return super.getItemUUID(position - 1);
    }

    @Override
    public int getPosition(String uuid) {
        int parent = super.getPosition(uuid);
        if (parent >= 0)
            return parent + 1;
        else
            return parent;
    }
}
