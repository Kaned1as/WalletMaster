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
    private Object originalData;

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

    public void setOriginalData(Object originalData) {
        this.originalData = originalData;
    }

    /**
     * Adds and applies action
     * @param type  type of action to add
     * @param entity optional entity (for deletion ID and entityType are needed)
     * @return result of inserting new operation
     */
    public static boolean makeAction(ActionType type, Object entity) {
        final EntityType entityType = entity.getClass().getAnnotation(EntityDescriptor.class).type();
        Log.d("makeAction", String.format("Entity type %s, action type %s", entityType.toString(), type.toString()));
        boolean status = false;
        mDatabase.beginTransaction();
        transactionFlow: {
            final ContentValues values = new ContentValues(3);
            values.put(ActionsFields.DATA_TYPE.toString(), entityType.ordinal());
            switch (type) {
                case ADD: { // we're adding, only store ID to keep track of it
                    // first, persist entity
                    final String persistedId = entity.persist(); // result holds ID now
                    if (persistedId == null) // error
                        break transactionFlow;
                    entity.setId(persistedId); // we should track new ID of entity in action fields

                    // second, do we have this data in any actions?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {entity.getId(), String.valueOf(entityType.ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) // we already have this entity deleted (reapplying operation), nothing to do
                        break;

                    values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                    values.put(ActionsFields.ORIGINAL_DATA.toString(), (byte[]) null);
                    final long actionRow = mDatabase.insert(ACTIONS_TABLE_NAME, null, values);
                    if(actionRow == -1)
                        break transactionFlow;
                    break;
                }
                case MODIFY: { // we are modifying, need to backup original!
                    // first, retrieve old entity
                    final Entity oldEntity;
                    switch (entity.getEntityType()) {
                        case ACCOUNTS:
                            oldEntity = Account.getFromDB(entity.getId());
                            break;
                        case CATEGORIES:
                            oldEntity = Category.getFromDB(entity.getId());
                            break;
                        case OPERATIONS:
                            oldEntity = Operation.getFromDB(entity.getId());
                            break;
                        case BUDGETS:
                            oldEntity = Budget.getFromDB(entity.getId());
                            break;
                        case BUDGET_ITEMS:
                            oldEntity = BudgetItem.getFromDB(entity.getId());
                            break;
                        default:
                            throw new IllegalArgumentException("No such entity type!" + entity.getEntityType());
                    }

                    // second, update old row with new data
                    final long rowsUpdated = entity.update(); // result holds updated entities count now
                    if (rowsUpdated != 1) // error
                        break transactionFlow;

                    // third, do we have original already?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {entity.getId(), String.valueOf(entityType.ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) // we already have this entity stored (added or modified), nothing to do
                        break;

                    values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                    values.put(ActionsFields.ORIGINAL_DATA.toString(), new Gson().toJson(oldEntity));
                    final long insertedActionId = mDatabase.insert(ACTIONS_TABLE_NAME, null, values);
                    if(insertedActionId == -1)
                        break transactionFlow;
                    break;
                }
                case DELETE: { // we are deleting need to store all original data
                    // first delete entity from DB
                    final long entitiesDeleted = entity.delete(); // result holds updated entities count now
                    if (entitiesDeleted != 1) // error
                        break transactionFlow;

                    // second, do we have this data in any actions?
                    final Cursor cursor = mDatabase.query(ACTIONS_TABLE_NAME, null, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entityType.ordinal())}, null, null, null, null);
                    if(cursor.moveToNext()) { // we already have this entity stored, added or modified, need to update previous action
                        final String backedEntity = cursor.getString(ActionsFields.ORIGINAL_DATA.ordinal());
                        if(backedEntity == null) { // if entity was added, we should just delete action, so all will look like nothing happened
                            final long actionsDeleted = mDatabase.delete(ACTIONS_TABLE_NAME, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entityType.ordinal())});
                            if(actionsDeleted != 1)
                                break transactionFlow;
                        } else { // was modified, change action type to deleted
                            values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                            final long actionsUpdated = mDatabase.update(ACTIONS_TABLE_NAME, values, ActionsFields.DATA_ID + " = ? AND " + ActionsFields.DATA_TYPE + " = ?", new String[] {String.valueOf(entity.getId()), String.valueOf(entityType.ordinal())});
                            if(actionsUpdated != 1)
                                break transactionFlow;
                        }
                    } else { // we don't have any actions, should create and store original
                        values.put(ActionsFields.DATA_ID.toString(), entity.getId());
                        values.put(ActionsFields.ORIGINAL_DATA.toString(), new Gson().toJson(entity));
                        final long actionRow = mDatabase.insert(ACTIONS_TABLE_NAME, null, values);
                        if(actionRow == -1)
                            break transactionFlow;
                    }
                    break;
                }
            }

            // all succeeded
            mDatabase.setTransactionSuccessful();
            status = true;
        }
        mDatabase.endTransaction();

        return status;
    }
}
