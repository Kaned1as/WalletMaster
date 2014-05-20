package com.adonai.wallet.entities;

import android.content.Context;
import android.database.Cursor;
import android.widget.BaseAdapter;

import java.util.UUID;

/**
 * Base class for all entity adapters
 * Makes its structure to be suitable for list views
 */
public abstract class UUIDCursorAdapter extends BaseAdapter {

    protected Cursor mCursor;
    protected Context mContext;

    private final int UUID_COLUMN = 0;

    public UUIDCursorAdapter(Context context, Cursor cursor) {
        mCursor = cursor;
        mContext = context;
    }

    public void changeCursor(Cursor cursor) {
        final Cursor old = swapCursor(cursor);
        if (old != null)
            old.close();
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        mCursor = newCursor;

        if (newCursor != null)
            notifyDataSetChanged();
        else
            notifyDataSetInvalidated();

        return oldCursor;
    }

    @Override
    public int getCount() {
        if (mCursor != null)
            return mCursor.getCount();
        else
            return 0;
    }

    @Override
    public Cursor getItem(int position) {
        if (mCursor != null) {
            mCursor.moveToPosition(position);
            return mCursor;
        } else
            return null;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor != null) {
            if (mCursor.moveToPosition(position))
                return UUID.fromString(mCursor.getString(UUID_COLUMN)).getLeastSignificantBits();
            else
                return 0;
        } else
            return 0;
    }

    public String getItemUUID(int position) {
        if (mCursor != null) {
            if (mCursor.moveToPosition(position))
                return mCursor.getString(UUID_COLUMN);
            else
                return null;
        } else
            return null;
    }

    public int getPosition(String id) {
        mCursor.moveToFirst();
        do {
            if(mCursor.getString(UUID_COLUMN).equals(id))
                return mCursor.getPosition();
        } while(mCursor.moveToNext());

        return -1;
    }
}
