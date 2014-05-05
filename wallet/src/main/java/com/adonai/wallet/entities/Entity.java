package com.adonai.wallet.entities;

import com.adonai.wallet.DatabaseDAO;

/**
 * Abstract type for all entities in DB
 */
public abstract class Entity {
    protected String id;
    protected final DatabaseDAO.EntityType entityType = getClass().getAnnotation(EntityDescriptor.class).type();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DatabaseDAO.EntityType getEntityType() {
        return entityType;
    }

    public abstract String persist(DatabaseDAO dao);

    public abstract int update(DatabaseDAO dao);

    public int delete(DatabaseDAO dao) {
        return dao.delete(getId(), entityType.toString());
    }
}
