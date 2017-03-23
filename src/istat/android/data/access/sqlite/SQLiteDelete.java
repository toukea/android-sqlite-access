package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteDatabase;

import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;

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

    public SQLiteAsyncExecutor.SQLiteThread<Integer> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

}
