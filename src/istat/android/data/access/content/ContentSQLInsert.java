package istat.android.data.access.content;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import istat.android.data.access.content.interfaces.QueryAble;
import istat.android.data.access.content.interfaces.ContentSQLClauseAble;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;
import istat.android.data.access.content.utils.ContentSQLThread;

public final class ContentSQLInsert implements ContentSQLClauseAble {
    List<QueryAble> modelInsertions = new ArrayList<QueryAble>();
    List<Object> insertions = new ArrayList<Object>();
    ContentSQL.SQL sql;

    ContentSQLInsert(ContentSQL.SQL sql) {
        this.sql = sql;
    }

    public ContentSQLInsert insert(Object insert) {
        try {
            QueryAble model = ContentSQLModel.fromObject(insert,
                    sql.getSerializer(insert.getClass()),
                    sql.getContentValueHandler(insert.getClass()));
            modelInsertions.add(model);
            insertions.add(insert);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ContentSQLInsert insert(boolean asTable, List<?> insert) {
        if (asTable) {
            return insertCollectionAsTable(insert);
        } else {
            return insertCollection(insert);
        }
    }

    private ContentSQLInsert insertCollection(Collection<?> insert) {
        if (insert == null || insert.isEmpty()) {
            return this;
        }
        for (Object obj : insert) {
            try {
                QueryAble model = ContentSQLModel.fromObject(obj,
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

    private ContentSQLInsert insertCollectionAsTable(List<?> insert) {
        if (insert == null) {
            return this;
        }
        try {
            QueryAble model = ContentSQLModel.fromObject(insert,
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

    public ContentSQLInsert insert(Collection<?> insert) {
        Class<?> cLass = insert.getClass();
        return insert(sql.isTableExist(cLass), new ArrayList(insert));
    }

    public ContentSQLInsert insert(Object... insert) {
        if (insert == null || insert.length == 0) {
            return this;
        }
        for (Object obj : insert) {
            try {
                QueryAble model = ContentSQLModel.fromObject(obj,
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
                tmp.add(insertion.insert(sql.getContentResolver()));
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
            out[index] = insertion.insert(sql.getContentResolver());
            index++;
        }
        modelInsertions.clear();
        notifyExecuted();
        return out;
    }

    public ContentSQLThread executeAsync() {
        return executeAsync(null);
    }

    public ContentSQLThread executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<long[]> callback) {
        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    private void notifyExecuted() {

    }

    public List<Object> getInsertions() {
        return insertions;
    }

    @Override
    public ContentSQL.SQL getInternalSQL() {
        return this.sql;
    }
}
