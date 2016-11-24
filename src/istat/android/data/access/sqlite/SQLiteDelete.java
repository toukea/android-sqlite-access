package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteDatabase;

public final class SQLiteDelete extends SQLiteClause<SQLiteDelete> {

    SQLiteDelete(Class<?> clazz, SQLite.SQL db) {
        super(clazz, db);
    }

    @Override
    protected Integer onExecute(SQLiteDatabase db) {
        return db.delete(table, getWhereClause(), getWhereParams());
    }

    public int execute() {
        return onExecute(sql.db);
    }

}
