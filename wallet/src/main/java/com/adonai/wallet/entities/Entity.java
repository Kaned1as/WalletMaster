package com.adonai.wallet.entities;

import com.adonai.wallet.DatabaseDAO;

/**
 * Created by Ochernovskiy on 11/04/2014.
 */
public abstract class Entity {
    protected final DatabaseDAO.EntityType entityType;
    protected Long id;

    public Entity(DatabaseDAO.EntityType entityType) {
        this.entityType = entityType;
    }

    public DatabaseDAO.EntityType getEntityType() {
        return entityType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public abstract long persist(DatabaseDAO dao);

    public abstract int update(DatabaseDAO dao);

    public abstract int delete(DatabaseDAO dao);
}
