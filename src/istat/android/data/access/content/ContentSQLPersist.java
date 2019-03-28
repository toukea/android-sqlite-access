package istat.android.data.access.content;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.content.interfaces.ContentSQLClauseAble;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;
import istat.android.data.access.content.utils.ContentSQLThread;

public final class ContentSQLPersist implements ContentSQLClauseAble {
    List<ContentSQLModel> modelPersist = new ArrayList<ContentSQLModel>();
    List<Object> persists = new ArrayList<Object>();
    ContentSQL.SQL sql;

    ContentSQLPersist(ContentSQL.SQL sql) {
        this.sql = sql;
    }

    public ContentSQLPersist persist(Object insert) {
        try {
            ContentSQLModel model = ContentSQLModel.fromObject(insert,
                    sql.getSerializer(insert.getClass()),
                    sql.getContentValueHandler(insert.getClass()));
            modelPersist.add(model);
            persists.add(insert);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ContentSQLPersist persist(List<?> insert) {
        for (Object obj : insert) {
            try {
                ContentSQLModel model = ContentSQLModel.fromObject(obj,
                        sql.getSerializer(insert.getClass()),
                        sql.getContentValueHandler(insert.getClass()));
                modelPersist.add(model);
                persists.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public ContentSQLPersist persist(Object... insert) {
        for (Object obj : insert) {
            try {
                ContentSQLModel model = ContentSQLModel.fromObject(obj,
                        sql.getSerializer(insert.getClass()),
                        sql.getContentValueHandler(insert.getClass()));
                modelPersist.add(model);
                persists.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute() throws SQLiteException {
        if (modelPersist == null || modelPersist.isEmpty()) {
            return new long[]{0};
        }
        long[] out = new long[modelPersist.size()];
        try {
            int index = 0;
            Object entity;
            for (ContentSQLModel insertion : modelPersist) {
                long value = insertion.persist(sql.getContentResolver());
                entity = persists.get(index);
                if (value > 0) {
                    //TODO update entity to match with new Id state.
                    insertion.flowInto(entity, insertion.getPrimaryKeyName());
                }
                out[index] = value;
                index++;
            }
            modelPersist.clear();
            notifyExecuted();
        } catch (Exception e) {
            SQLiteException error = new SQLiteException(e.getMessage());
            error.initCause(e);
            error.setStackTrace(e.getStackTrace());
            throw error;
        }
        return out;
    }

//    public List<Object> execute() {
//        List<Object> entities = new ArrayList<>();
//        if (modelPersist == null || modelPersist.size() == 0)
//            return entities;
//        int index = 0;
//        for (ContentSQLModel insertion : modelPersist) {
//            long out = insertion.persist(sql.db);
//            Object entity = persists.get(index);
//            if (out > 0) {
//                //TODO update entity to match with new Id state.
//                insertion.flowInto(entity, insertion.getPrimaryKeyName());
//            }
//            entities.add(entity);
//            index++;
//        }
//        modelPersist.clear();
//        notifyExecuted();
//        return entities;
//    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }

    @Override
    public ContentSQL.SQL getInternalSQL() {
        return this.sql;
    }

    public List<Object> getPersists() {
        return persists;
    }

    public ContentSQLThread executeAsync() {
        return executeAsync(null);
    }

    public ContentSQLThread executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<long[]> callback) {
        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

//    public ContentSQLThread executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<List<Object>> callback) {
//        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
//        return asyncExecutor.execute(this, callback);
//    }
}
