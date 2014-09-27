package com.adonai.wallet.entities;

import android.content.Context;
import android.database.Cursor;
import android.widget.BaseAdapter;

import java.util.UUID;

/**
 * Base class for all entity adapters
 * Makes its structure to be suitable for list views
 *
 * Implements methods to handle string ID columns
 *
 * @author Adonai
 */
public abstract class UUIDCursorAdapter extends BaseAdapter {

    protected Cursor mCursor;
    protected Context mContext;

    private final int uuidColumn;

    public UUIDCursorAdapter(Context context, Cursor cursor) {
        mCursor = cursor;
        mContext = context;
        uuidColumn = cursor.getColumnIndex("id");
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
                return UUID.fromString(mCursor.getString(uuidColumn)).getLeastSignificantBits();
            else
                return 0;
        } else
            return 0;
    }

    public UUID getItemUUID(int position) {
        if (mCursor != null) {
            if (mCursor.moveToPosition(position))
                return UUID.fromString(mCursor.getString(uuidColumn));
            else
                return null;
        } else
            return null;
    }

    public int getPosition(String uuid) {
        mCursor.moveToFirst();
        do {
            if(mCursor.getString(uuidColumn).equals(uuid))
                return mCursor.getPosition();
        } while(mCursor.moveToNext());

        return -1;
    }

    public int getPosition(UUID uuid) {
        String strRepresentation = uuid.toString();
        return getPosition(strRepresentation);
    }
}
