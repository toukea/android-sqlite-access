package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteDelete extends SQLiteClause<SQLiteDelete> {

    SQLiteDelete(Class<?> clazz, SQLite.SQL sql) {
        super(clazz, sql);
    }

    @Override
    protected Integer onExecute(SQLiteDatabase db) {
        String whereClause = getWhereClause();
        String[] whereParams = getWhereParams();
        return db.delete(table, whereClause, whereParams);
    }

    public int execute() {
        int out = onExecute(sql.db);
        notifyExecuted();
        return out;
    }

    public SQLiteThread<Integer> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    @Override
    public String getStatement() {
        String out = "DELETE FROM " + table;
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE '" + whereClause.trim() + "'";
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

}
