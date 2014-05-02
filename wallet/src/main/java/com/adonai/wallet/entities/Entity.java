package com.adonai.wallet.entities;

import com.adonai.wallet.DatabaseDAO;

/**
 * Abstract type for all entities in DB
 */
public abstract class Entity {
    protected Long id;
    protected final DatabaseDAO.EntityType entityType = getClass().getAnnotation(EntityDescriptor.class).type();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatabaseDAO.EntityType getEntityType() {
        return entityType;
    }

    public abstract long persist(DatabaseDAO dao);

    public abstract int update(DatabaseDAO dao);

    public int delete(DatabaseDAO dao) {
        return dao.delete(getId(), entityType.toString());
    }
}
