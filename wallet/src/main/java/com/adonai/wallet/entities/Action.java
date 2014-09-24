package com.adonai.wallet.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by adonai on 23.09.14.
 */
@DatabaseTable
public class Action {

    public static enum ActionType {
        ADD,
        DELETE,
        MODIFY
    }

    @DatabaseField(id = true, useGetSet = true)
    private String dataId;

    @DatabaseField(canBeNull = false)
    private ActionType dataType;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private Entity originalData;

    public String getDataId() {
        return dataId + "/" + dataType;
    }

    public String getDataIdRaw() {
        return dataId;
    }

    public void setDataIdRaw(String dataId) {
        this.dataId = dataId;
    }

    public void setDataId(String dataId) {
        String[] contents = dataId.split("/");
        this.dataId = contents[0];
        this.dataType = ActionType.valueOf(contents[1]);
    }

    public ActionType getDataType() {
        return dataType;
    }

    public void setDataType(ActionType dataType) {
        this.dataType = dataType;
    }

    public Object getOriginalData() {
        return originalData;
    }

    public void setOriginalData(Entity originalData) {
        this.originalData = originalData;
    }

    /**
     * Adds and applies action
     * @param type  type of action to add
     * @param entity optional entity (for deletion ID and entityType are needed)
     * @return result of inserting new operation
     */
    public static boolean makeAction(ActionType type, Entity entity) {
        switch (type) {
            case ADD:

                break;
            case MODIFY:
                break;
            case DELETE:
                break;
        }
    }
}
