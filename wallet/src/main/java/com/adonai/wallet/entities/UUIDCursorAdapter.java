package com.adonai.wallet.entities;

import android.content.Context;
import android.widget.BaseAdapter;

import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Base class for all entity adapters
 * Makes its structure to be suitable for list views
 *
 * Implements methods to handle string ID columns
 *
 * @author Adonai
 */
public abstract class UUIDCursorAdapter<T extends Entity> extends BaseAdapter {

    private EntityDao<T> mDao;

    protected CloseableIterator<T> mCursor;
    protected Context mContext;

    public UUIDCursorAdapter(Context context, EntityDao<T> dao) {
        mDao = dao;
        mContext = context;
        mDao.registerObserver(this);
        mCursor = mDao.iterator();

    }

    @Override
    public int getCount() {
        return ((AndroidDatabaseResults) mCursor.getRawResults()).getCount();
    }

    @Override
    public T getItem(int position) {
        try {
            mCursor.first();
            return mCursor.moveRelative(position);
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        try {
            mCursor.first();
            T entity = mCursor.moveRelative(position);
            return entity.getId().getLeastSignificantBits();
        } catch (SQLException e) {
            return  -1;
        }
    }

    public UUID getItemUUID(int position) {
        try {
            mCursor.first();
            T entity = mCursor.moveRelative(position);
            return entity.getId();
        } catch (SQLException e) {
            return  null;
        }
    }

    public int getPosition(String uuid) {
        UUID toFind = UUID.fromString(uuid);
        return getPosition(toFind);
    }

    public int getPosition(UUID uuid) {
        try {
            int pos = 0;
            T first = mCursor.first();
            if(first.getId().equals(uuid))
                return pos;

            while (mCursor.hasNext()) {
                ++pos;
                T entity = mCursor.next();
                if(entity.getId().equals(uuid))
                    return pos;
            }
        } catch (SQLException e) {
            return  -1;
        }
        return -1;
    }

    public void setQuery(PreparedQuery<T> query) {
        try {
            mCursor = mDao.iterator(query);
            notifyDataSetChanged();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeCursor() {
        mCursor.closeQuietly();
        mDao.unregisterObserver(this);
    }
}
