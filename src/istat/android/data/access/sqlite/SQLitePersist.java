package istat.android.data.access.sqlite;

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

    public long[] execute() {
        if (modelPersist == null || modelPersist.size() == 0)
            return new long[]{0};
        long[] out = new long[modelPersist.size()];
        int index = 0;
        for (QueryAble insertion : modelPersist) {
            out[index] = insertion.persist(sql.db);
            index++;
        }
        modelPersist.clear();
        notifyExecuted();
        return out;
    }

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
}
