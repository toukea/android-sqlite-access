package istat.android.data.access.content;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import istat.android.data.access.content.utils.ContentSQLThread;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;

public final class ContentSQLDelete extends ContentSQLClause<ContentSQLDelete> {

    ContentSQLDelete(Class<?> clazz, ContentSQL.SQL sql) {
        super(clazz, sql);
    }

    @Override
    protected Integer onExecute(ContentResolver db) {
        notifyExecuting();
        String whereClause = getWhereClause();
        String queryEnding = (TextUtils.isEmpty(orderBy) ? "" : " ORDER BY " + orderBy)
                + (TextUtils.isEmpty(orderBy) ? "" : " LIMIT " + limit);
        if (!TextUtils.isEmpty(queryEnding)) {
            if (whereClause == null) {
                whereClause = queryEnding;
            } else {
                whereClause += queryEnding;
            }
        }
        String[] whereParams = getWhereParams();
        return db.delete(sql.dataUri, whereClause, whereParams);
    }

    public int execute(int limit) {
        limit(limit);
        return execute();
    }


    public int execute() {
        int out = onExecute(sql.getContentResolver());
        notifyExecuted();
        return out;
    }

    public ContentSQLThread<Integer> executeAsync() {
        return executeAsync(null);
    }

    public ContentSQLThread<Integer> executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<Integer> callback) {
        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    @Override
    public String getStatement() {
        String out = "DELETE FROM " + table;
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE '" + whereClause.toString().trim() + "'";
        }
        String[] splits = out.split("\\?");
        String sql = "";
        for (int i = 0; i < (!out.endsWith("?") ? splits.length - 1
                : splits.length); i++) {
            sql += splits[i];
            sql += "'" + whereParams.get(i) + "'";
        }
        if (!out.endsWith("?")) {
            sql += splits[splits.length - 1];
        }
        return sql;
    }

    public ContentSQLDelete.SQLiteDeleteLimit limit(int limit) {
        String limitS = null;
        if (limit > 0) {
            limitS = " " + limit;
        }
        return new ContentSQLDelete.SQLiteDeleteLimit(limitS);
    }

    public class SQLiteDeleteLimit {
        SQLiteDeleteLimit(String limitS) {
            ContentSQLDelete.this.limit = limitS;
        }

        public int execute() {
            return ContentSQLDelete.this.execute();
        }

        public int execute(int limit) {
            return ContentSQLDelete.this.execute(limit);
        }

        public ContentSQLThread<Integer> executeAsync() {
            return ContentSQLDelete.this.executeAsync();
        }

        public ContentSQLThread<Integer> executeAsync(ContentSQLAsyncExecutor.ExecutionCallback<Integer> callback) {
            return ContentSQLDelete.this.executeAsync(callback);
        }

        public String getStatement() {
            return ContentSQLDelete.this.getStatement();
        }
    }

}
