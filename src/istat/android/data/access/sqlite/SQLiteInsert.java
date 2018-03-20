package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import istat.android.data.access.sqlite.interfaces.QueryAble;
import istat.android.data.access.sqlite.interfaces.SQLiteClauseAble;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteInsert implements SQLiteClauseAble {
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

    public SQLiteInsert insert(boolean asTable, List<?> insert) {
        if (asTable) {
            return insertCollectionAsTable(insert);
        } else {
            return insertCollection(insert);
        }
    }

    private SQLiteInsert insertCollection(Collection<?> insert) {
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

    private SQLiteInsert insertCollectionAsTable(List<?> insert) {
        if (insert == null) {
            return this;
        }
        try {
            QueryAble model = SQLiteModel.fromObject(insert,
                    sql.getSerializer(insert.getClass()),
                    sql.getContentValueHandler(insert.getClass()));
            modelInsertions.add(model);
            insertions.add(insert);
            if (!insert.isEmpty()) {
                Object item0 = insert.get(0);
                if (sql.isTableExist(item0.getClass())) {
                    insertCollection(insert);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteInsert insert(Collection<?> insert) {
        Class<?> cLass = insert.getClass();
        return insert(sql.isTableExist(cLass), new ArrayList(insert));
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

    public long[] execute(boolean ignoreOnDuplicate) throws SQLiteException {
        List<Long> tmp = new ArrayList<Long>();
        try {
            if (modelInsertions == null || modelInsertions.size() == 0)
                return new long[]{0};
            for (QueryAble insertion : modelInsertions) {
//                if (insertion.exist(sql.db)) {
//                    throw new IllegalAccessException("entity :0" + insertion + " already exist inside table " + insertion.getName());
//                }
                tmp.add(insertion.insert(sql.db));
            }
            modelInsertions.clear();
            notifyExecuted();
        } catch (Exception e) {
            e.printStackTrace();
            if (!ignoreOnDuplicate) {
                SQLiteException error = new SQLiteException(e.getMessage());
                error.initCause(e);
                throw new RuntimeException(e);
            }
        }
        long[] out = new long[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            out[i] = tmp.get(i);
        }
        return out;
    }

    public long[] execute() throws SQLiteException {
        if (modelInsertions == null || modelInsertions.size() == 0)
            return new long[]{0};
        long[] out = new long[modelInsertions.size()];
        int index = 0;
        for (QueryAble insertion : modelInsertions) {
//            if (insertion.exist(sql.db)) {
//                throw new IllegalAccessException("entity :0" + insertion + " already exist inside table " + insertion.getName());
//            }
            out[index] = insertion.insert(sql.db);
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

    @Override
    public SQLite.SQL getInternalSQL() {
        return this.sql;
    }
}
