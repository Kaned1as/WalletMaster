package com.adonai.wallet.database;

import android.database.Observable;

import com.adonai.wallet.entities.Entity;
import com.adonai.wallet.entities.UUIDCursorAdapter;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Basic dao needed for persisting changes locally
 */
public class EntityDao<T extends Entity> extends BaseDaoImpl<T, UUID> {

    private DbNotifier mObservable = new DbNotifier();

    public EntityDao(Class<T> dataClass) throws SQLException {
        super(dataClass);
    }

    public EntityDao(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public EntityDao(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    @Override
    public int create(T data) throws SQLException {
        int result;
        data.setDeleted(false);
        data.setBackup(null);
        result = super.create(data);
        mObservable.notifyObservers();
        return result;
    }

    @Override
    public int update(T data) throws SQLException {
        int result;
        if(data.getLastModified() == null) { // never been synced, safe to update
            result = super.update(data);
        } else {                             // exists on the server side, set dirty (if not already)
            T base = queryForSameId(data);
            if(!base.isDirty())              // we have backed up version, no need to update it (as it's purpose is to keep original)
                data.setBackup(base);
            result = super.update(data);
        }
        mObservable.notifyObservers();
        return result;
    }

    @Override
    public int delete(T data) throws SQLException {
        int result;
        if(data.getLastModified() == null) { // never been synced, safe to delete
            result = super.delete(data);
        } else {                             // exists on the server side, set deleted
            T base = queryForSameId(data);
            if(!base.isDirty())              // we have backed up version, no need to update it (as it's purpose is to keep original)
                data.setBackup(base);
            data.setDeleted(true);
            result = super.update(data);
        }
        mObservable.notifyObservers();
        return result;
    }

    @Override
    public int deleteById(UUID uuid) throws SQLException {
        int result;
        T data = queryForId(uuid);
        if(data.getLastModified() == null) { // never been synced, safe to delete
            result = super.deleteById(uuid);
        } else {                             // exists on the server side, set deleted
            T base = queryForSameId(data);
            if(!base.isDirty())              // we have backed up version, no need to update it (as it's purpose is to keep original)
                data.setBackup(base);
            data.setDeleted(true);
            result = super.update(data);
        }
        mObservable.notifyObservers();
        return result;
    }

    public int deleteByServer(T data) { // entity should be deleted on client as on server
        try {
            int result = super.delete(data);
            mObservable.notifyObservers();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int createByServer(T data) {
        try {
            data.setBackup(null);
            int result = super.create(data);
            mObservable.notifyObservers();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int updateByServer(T data) {
        try {
            data.setBackup(null);
            int result = super.update(data);
            mObservable.notifyObservers();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerObserver(UUIDCursorAdapter<T> observer) {
        mObservable.registerObserver(observer);
    }

    public void unregisterObserver(UUIDCursorAdapter<T> observer) {
        mObservable.unregisterObserver(observer);
    }

    public void unregisterAll() {
        mObservable.unregisterAll();
    }

    private class DbNotifier extends Observable<UUIDCursorAdapter<T>> {

        public void notifyObservers() {
            for(UUIDCursorAdapter<T> observer : mObservers) {
                observer.notifyDataSetChanged();
            }
        }

    }
}
