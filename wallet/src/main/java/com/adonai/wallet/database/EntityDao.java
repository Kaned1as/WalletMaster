package com.adonai.wallet.database;

import com.adonai.wallet.entities.Entity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Basic dao needed for persisting changes locally
 */
public class EntityDao<T extends Entity> extends BaseDaoImpl<T, UUID> {

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
        data.setDeleted(false);
        data.setDirty(false);
        return super.create(data);
    }

    @Override
    public int update(T data) throws SQLException {
        if(data.getLastModified() == null) { // never been synced, safe to update
            return super.update(data);
        } else {                             // exists on the server side, set dirty
            data.setDirty(true);
            return super.update(data);
        }
    }

    @Override
    public int delete(T data) throws SQLException {
        if(data.getLastModified() == null) { // never been synced, safe to delete
            return super.delete(data);
        } else {                             // exists on the server side, set deleted
            data.setDeleted(true);
            return super.update(data);
        }
    }

    @Override
    public int deleteById(UUID uuid) throws SQLException {
        T data = queryForId(uuid);
        if(data.getLastModified() == null) { // never been synced, safe to delete
            return super.deleteById(uuid);
        } else {                             // exists on the server side, set deleted
            data.setDeleted(true);
            return super.update(data);
        }
    }

    public int deleteByServer(T data) throws SQLException { // entity should be deleted on client as on server
        return super.delete(data);
    }

}
