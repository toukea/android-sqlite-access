package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;

public final class SQLiteInsert {
    List<QueryAble> insertions = new ArrayList<QueryAble>();
    SQLite.SQL sql;

    SQLiteInsert(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLiteInsert insert(Object insert) {
        try {
            QueryAble model = SQLiteModel.fromObject(insert);
            insertions.add(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteInsert insert(List<?> insert) {
        for (Object obj : insert) {
            try {
                QueryAble model = SQLiteModel.fromObject(obj);
                insertions.add(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLiteInsert insert(Object... insert) {
        for (Object obj : insert) {
            try {
                QueryAble model = SQLiteModel.fromObject(obj);
                insertions.add(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute(boolean ignoreOnDuplicate) {
        List<Long> tmp = new ArrayList<Long>();
        try {
            if (insertions == null || insertions.size() == 0)
                return new long[]{0};
            for (QueryAble insertion : insertions) {
                if (insertion.exist(sql.db)) {
                    throw new IllegalAccessException("entity :0" + insertion + " already exist inside table " + insertion.getName());
                }
                tmp.add(insertion.persist(sql.db));
            }
            insertions.clear();
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
        if (insertions == null || insertions.size() == 0)
            return new long[]{0};
        long[] out = new long[insertions.size()];
        int index = 0;
        for (QueryAble insertion : insertions) {
            if (insertion.exist(sql.db)) {
                throw new IllegalAccessException("entity :0" + insertion + " already exist inside table " + insertion.getName());
            }
            out[index] = insertion.persist(sql.db);
        }
        insertions.clear();
        notifyExecuted();
        return out;
    }

    public SQLiteAsyncExecutor.SQLiteThread executeAsync(final int offset, final int limit, final SQLiteAsyncExecutor.ExecutionCallback<long[]> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }
}
