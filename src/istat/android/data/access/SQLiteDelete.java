package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class SQLiteDelete extends SQLiteClause<SQLiteDelete> {
    SQLiteDelete(String table, SQLiteDatabase db) {
        super(table, null, db);
    }

    SQLiteDelete(Class<?> clazz, SQLiteDatabase db) {
        super(clazz, db);
    }

    @Override
    protected Integer onExecute(SQLiteDatabase db) {
        return db.delete(table, getWhereClose(), getWhereParams());
    }

    public int execute() {
        return onExecute(db);
    }

}
