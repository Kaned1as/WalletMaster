package com.adonai.wallet.adapters;

import android.app.Activity;
import android.widget.BaseAdapter;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.entities.Entity;
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

    protected final EntityDao<T> mDao;
    protected final Activity mContext;

    protected PreparedQuery<T> mQuery = null;
    protected CloseableIterator<T> mCursor;

    private final Where<T, UUID> defaultWhere;

    public UUIDCursorAdapter(Activity context, Class<T> clazz) {
        try {
            mDao = DbProvider.getHelper().getDao(clazz);
            mContext = context;
            defaultWhere = mDao.queryBuilder().where().eq("deleted", false);
            mCursor = defaultWhere.iterator();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor for cursor adapter with specific where
     * @param context context to build objects on
     */
    public UUIDCursorAdapter(Activity context, Class<T> clazz, Where<T, UUID> where) {
        try {
            mDao = DbProvider.getHelper().getDao(clazz);
            mContext = context;
            defaultWhere = where.and().eq("deleted", false);
            mCursor = defaultWhere.iterator();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor for cursor adapter with specific ordering of objects
     * @param context context to build objects on
     * @param builder builder with specific ordering
     */
    public UUIDCursorAdapter(Activity context, Class<T> clazz, QueryBuilder<T, UUID> builder) {
        try {
            // set dao and context
            mDao = DbProvider.getHelper().getDao(clazz);
            mContext = context;
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
            ((AndroidDatabaseResults) mCursor.getRawResults()).moveAbsolute(position);
            T entity = mCursor.current();
            if(entity == null)
                return -1;

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
            mQuery = query;
            mCursor = mQuery != null ? mDao.iterator(mQuery) : defaultWhere.iterator();
            notifyDataSetChanged();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setQuery(QueryBuilder<T, UUID> qBuilder) {
        try {
            mQuery = qBuilder != null ? qBuilder.prepare() : null;
            notifyDataSetChanged();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeCursor() {
        mCursor.closeQuietly();
    }
}
