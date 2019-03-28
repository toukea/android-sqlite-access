package istat.android.data.access.content;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.content.interfaces.ContentSQLClauseAble;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;
import istat.android.data.access.content.utils.ContentSQLThread;

public final class ContentSQLMerge implements ContentSQLClauseAble {
    List<ContentSQLModel> modelMerges = new ArrayList<ContentSQLModel>();
    List<Object> merges = new ArrayList<Object>();
    ContentSQL.SQL sql;

    ContentSQLMerge(ContentSQL.SQL sql) {
        this.sql = sql;
    }

    public ContentSQLMerge merge(Object merge) {
        try {
            ContentSQLModel model = ContentSQLModel.fromObject(merge,
                    sql.getSerializer(merge.getClass()),
                    sql.getContentValueHandler(merge.getClass()));
            modelMerges.add(model);
            merges.add(merge);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ContentSQLMerge merge(List<?> entities) {
        for (Object merge : entities) {
            try {
                ContentSQLModel model = ContentSQLModel.fromObject(merge,
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

    public ContentSQLMerge merge(Object... merge) {
        for (Object obj : merge) {
            try {
                ContentSQLModel model = ContentSQLModel.fromObject(obj,
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
            for (ContentSQLModel merge : modelMerges) {
                long out = merge.merge(sql.getContentResolver());
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

    public ContentSQLThread executeAsync() {
        return executeAsync(null);
    }

    public ContentSQLThread executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<List<Object>> callback) {
        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    @Override
    public ContentSQL.SQL getInternalSQL() {
        return this.sql;
    }
}
