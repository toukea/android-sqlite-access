package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.List;

import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteDelete extends SQLiteClause<SQLiteDelete> {

    SQLiteDelete(Class<?> clazz, SQLite.SQL sql) {
        super(clazz, sql);
    }

    @Override
    protected Integer onExecute(SQLiteDatabase db) {
        notifyExecuting();
        String whereClause = getWhereClause();
        whereClause += (TextUtils.isEmpty(orderBy) ? "" : " ORDER BY " + orderBy)
                + (TextUtils.isEmpty(orderBy) ? "" : " LIMIT " + limit);
        String[] whereParams = getWhereParams();
        return db.delete(table, whereClause, whereParams);
    }

    public int execute(int limit) {
        limit(limit);
        return execute();
    }


    public int execute() {
        int out = onExecute(sql.db);
        notifyExecuted();
        return out;
    }

    public SQLiteThread<Integer> executeAsync() {
        return executeAsync(null);
    }

    public SQLiteThread<Integer> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
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

    public SQLiteDelete.SQLiteDeleteLimit limit(int limit) {
        String limitS = null;
        if (limit > 0) {
            limitS = " " + limit;
        }
        return new SQLiteDelete.SQLiteDeleteLimit(limitS);
    }

    public class SQLiteDeleteLimit {
        SQLiteDeleteLimit(String limitS) {
            SQLiteDelete.this.limit = limitS;
        }

        public int execute() {
            return SQLiteDelete.this.execute();
        }

        public int execute(int limit) {
            return SQLiteDelete.this.execute(limit);
        }

        public SQLiteThread<Integer> executeAsync() {
            return SQLiteDelete.this.executeAsync();
        }

        public SQLiteThread<Integer> executeAsync(SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
            return SQLiteDelete.this.executeAsync(callback);
        }

        public String getStatement() {
            return SQLiteDelete.this.getStatement();
        }
    }

}
