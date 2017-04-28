package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.sqlite.interfaces.QueryAble;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteInsert {
    List<QueryAble> modelInsertions = new ArrayList<QueryAble>();
    List<Object> insertions = new ArrayList<Object>();
    SQLite.SQL sql;

    SQLiteInsert(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLiteInsert insert(Object insert) {
        try {
            QueryAble model = SQLiteModel.fromObject(insert,
                    sql.getSerializer(insert.getClass()),
                    sql.getContentValueHandler(insert.getClass()));
            modelInsertions.add(model);
            insertions.add(insert);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteInsert insert(List<?> insert) {
        if (insert == null || insert.isEmpty()) {
            return this;
        }
        for (Object obj : insert) {
            try {
                QueryAble model = SQLiteModel.fromObject(obj,
                        sql.getSerializer(insert.getClass()),
                        sql.getContentValueHandler(insert.getClass()));
                modelInsertions.add(model);
                insertions.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLiteInsert insert(Object... insert) {
        if (insert == null || insert.length == 0) {
            return this;
        }
        for (Object obj : insert) {
            try {
                QueryAble model = SQLiteModel.fromObject(obj,
                        sql.getSerializer(insert.getClass()),
                        sql.getContentValueHandler(insert.getClass()));
                modelInsertions.add(model);
                insertions.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute(boolean ignoreOnDuplicate) {
        List<Long> tmp = new ArrayList<Long>();
        try {
            if (modelInsertions == null || modelInsertions.size() == 0)
                return new long[]{0};
            for (QueryAble insertion : modelInsertions) {
                if (insertion.exist(sql.db)) {
                    throw new IllegalAccessException("entity :0" + insertion + " already exist inside table " + insertion.getName());
                }
                tmp.add(insertion.persist(sql.db));
            }
            modelInsertions.clear();
            notifyExecuted();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            if (!ignoreOnDuplicate) {
                throw new RuntimeException(e);
            }
        }
        long[] out = new long[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            out[i] = tmp.get(i);
        }
        return out;
    }

    public long[] execute() throws IllegalAccessException {
        if (modelInsertions == null || modelInsertions.size() == 0)
            return new long[]{0};
        long[] out = new long[modelInsertions.size()];
        int index = 0;
        for (QueryAble insertion : modelInsertions) {
            if (insertion.exist(sql.db)) {
                throw new IllegalAccessException("entity :0" + insertion + " already exist inside table " + insertion.getName());
            }
            out[index] = insertion.persist(sql.db);
            index++;
        }
        modelInsertions.clear();
        notifyExecuted();
        return out;
    }

    public SQLiteThread executeAsync() {
        return executeAsync(null);
    }

    public SQLiteThread executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<long[]> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }

    public List<Object> getInsertions() {
        return insertions;
    }
}
