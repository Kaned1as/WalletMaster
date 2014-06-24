package com.adonai.wallet.entities;

import com.adonai.wallet.DatabaseDAO;

/**
 * Abstract type for all entities in DB
 *
 * @author Adonai
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

    public abstract String persist();

    public abstract int update();

    public int delete() {
        return DatabaseDAO.getInstance().delete(getId(), entityType.toString());
    }
}
