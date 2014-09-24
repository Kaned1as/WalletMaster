package com.adonai.wallet.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import java.util.Date;
import java.util.UUID;

/**
 * Created by adonai on 24.09.14.
 */
public class Entity {

    @DatabaseField(id = true)
    private UUID id = UUID.randomUUID();

    @DatabaseField(canBeNull = true, dataType = DataType.DATE_LONG)
    private Date lastModified;

    @DatabaseField
    private boolean deleted;

    @DatabaseField
    private boolean dirty; // indicates the entity is changed or not

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
