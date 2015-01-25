package com.adonai.wallet.entities;

import android.app.Activity;
import android.widget.BaseAdapter;

import com.adonai.wallet.database.EntityDao;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

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

    private final EntityDao<T> mDao;
    protected final Activity mContext;

    protected PreparedQuery<T> mQuery = null;
    protected CloseableIterator<T> mCursor;

    private final Where<T, UUID> defaultWhere;

    public UUIDCursorAdapter(Activity context, EntityDao<T> dao) {
        try {
            mDao = dao;
            mContext = context;
            mDao.registerObserver(this);
            defaultWhere = mDao.queryBuilder().where().eq("deleted", false);
            mCursor = defaultWhere.iterator();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor for cursor adapter with specific ordering of objects
     * @param context context to build objects on
     * @param dao entity dao for objects
     * @param builder builder with specific ordering
     */
    public UUIDCursorAdapter(Activity context, EntityDao<T> dao, QueryBuilder<T, UUID> builder) {
        try {
            // set dao and context
            mDao = dao;
            mContext = context;
            mDao.registerObserver(this);
            defaultWhere = builder.where().eq("deleted", false);
            mQuery = defaultWhere.prepare(); // don't query objects marked as deleted
            mCursor = mDao.iterator(mQuery);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getCount() {
        return ((AndroidDatabaseResults) mCursor.getRawResults()).getCount();
    }

    @Override
    public T getItem(int position) {
        try {
            ((AndroidDatabaseResults) mCursor.getRawResults()).moveAbsolute(position);
            return mCursor.current();
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
        mQuery = query;
        notifyDataSetChanged();
    }

    public void setQuery(QueryBuilder<T, UUID> qBuilder) {
        try {
            mQuery = qBuilder != null ? qBuilder.prepare() : null;
            notifyDataSetChanged();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mCursor = (mQuery != null) ? mDao.iterator(mQuery) : defaultWhere.iterator();
                    UUIDCursorAdapter.super.notifyDataSetChanged();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void closeCursor() {
        mCursor.closeQuietly();
        mDao.unregisterObserver(this);
    }
}
