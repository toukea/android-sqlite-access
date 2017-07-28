package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.sqlite.interfaces.SQLiteClauseAble;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteMerge implements SQLiteClauseAble {
    List<SQLiteModel> modelMerges = new ArrayList<SQLiteModel>();
    List<Object> merges = new ArrayList<Object>();
    SQLite.SQL sql;

    SQLiteMerge(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLiteMerge merge(Object merge) {
        try {
            SQLiteModel model = SQLiteModel.fromObject(merge,
                    sql.getSerializer(merge.getClass()),
                    sql.getContentValueHandler(merge.getClass()));
            modelMerges.add(model);
            merges.add(merge);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteMerge merge(List<?> entities) {
        for (Object merge : entities) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(merge,
                        sql.getSerializer(merge.getClass()),
                        sql.getContentValueHandler(merge.getClass()));
                modelMerges.add(model);
                merges.add(merge);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLiteMerge merge(Object... merge) {
        for (Object obj : merge) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,
                        sql.getSerializer(merge.getClass()),
                        sql.getContentValueHandler(merge.getClass()));
                modelMerges.add(model);
                merges.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public List<Object> execute() throws SQLiteException {
        List<Object> entities = new ArrayList<>();
        try {
            if (modelMerges == null || modelMerges.isEmpty())
                return entities;
            int index = 0;
            for (SQLiteModel merge : modelMerges) {
                long out = merge.merge(sql.db);
                Object entity = merges.get(index);
                if (out >= 0) {
                    //TODO update entity to match with new Id state.
                    merge.flowInto(entity, merge.getColumns());
                }
                entities.add(entity);
                index++;
            }
            modelMerges.clear();
            notifyExecuted();
        } catch (Exception e) {
            SQLiteException error = new SQLiteException(e.getMessage());
            error.initCause(e);
            error.setStackTrace(e.getStackTrace());
            throw error;
        }
        return entities;
    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }

    public List<Object> getMerges() {
        return merges;
    }

    public SQLiteThread executeAsync() {
        return executeAsync(null);
    }

    public SQLiteThread executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<List<Object>> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    @Override
    public SQLite.SQL getInternalSQL() {
        return this.sql;
    }
}
