package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.sqlite.interfaces.QueryAble;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLitePersist {
    List<SQLiteModel> modelPersist = new ArrayList<SQLiteModel>();
    List<Object> persists = new ArrayList<Object>();
    SQLite.SQL sql;

    SQLitePersist(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLitePersist persist(Object insert) {
        try {
            SQLiteModel model = SQLiteModel.fromObject(insert,
                    sql.getSerializer(insert.getClass()),
                    sql.getContentValueHandler(insert.getClass()));
            modelPersist.add(model);
            persists.add(insert);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLitePersist persist(List<?> insert) {
        for (Object obj : insert) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,
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

    public SQLitePersist persist(Object... insert) {
        for (Object obj : insert) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,
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
        if (modelPersist == null || modelPersist.size() == 0)
            return new long[]{0};
        long[] out = new long[modelPersist.size()];
        try {
            int index = 0;
            for (SQLiteModel insertion : modelPersist) {
                long value = insertion.persist(sql.db);
                Object entity = persists.get(index);
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
//        for (SQLiteModel insertion : modelPersist) {
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

    public List<Object> getPersists() {
        return persists;
    }

    public SQLiteThread executeAsync() {
        return executeAsync(null);
    }

    public SQLiteThread executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<long[]> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

//    public SQLiteThread executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<List<Object>> callback) {
//        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
//        return asyncExecutor.execute(this, callback);
//    }
}
