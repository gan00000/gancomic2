package com.ccsky.sfish.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "FILTER".
*/
public class FilterDao extends AbstractDao<Filter, Long> {

    public static final String TABLENAME = "FILTER";

    /**
     * Properties of entity Filter.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Mode = new Property(1, int.class, "mode", false, "MODE");
        public final static Property Text = new Property(2, String.class, "text", false, "TEXT");
        public final static Property Enable = new Property(3, Boolean.class, "enable", false, "ENABLE");
    };


    public FilterDao(DaoConfig config) {
        super(config);
    }
    
    public FilterDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"FILTER\" (" + //
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"MODE\" INTEGER NOT NULL ," + // 1: mode
                "\"TEXT\" TEXT," + // 2: text
                "\"ENABLE\" INTEGER);"); // 3: enable
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"FILTER\"";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Filter entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getMode());
 
        String text = entity.getText();
        if (text != null) {
            stmt.bindString(3, text);
        }
 
        Boolean enable = entity.getEnable();
        if (enable != null) {
            stmt.bindLong(4, enable ? 1L: 0L);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public Filter readEntity(Cursor cursor, int offset) {
        Filter entity = new Filter( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getInt(offset + 1), // mode
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // text
            cursor.isNull(offset + 3) ? null : cursor.getShort(offset + 3) != 0 // enable
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Filter entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setMode(cursor.getInt(offset + 1));
        entity.setText(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setEnable(cursor.isNull(offset + 3) ? null : cursor.getShort(offset + 3) != 0);
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Filter entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(Filter entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}