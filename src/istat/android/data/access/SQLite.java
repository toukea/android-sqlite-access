package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class SQLite {
    public static SQLiteStatement from(SQLiteDatabase db) {
        return new SQLiteStatement(db);
    }

    public static class SQLiteStatement {
        SQLiteDatabase db;

        SQLiteStatement(SQLiteDatabase db) {
            this.db = db;
        }

        public SQLiteSelect select(Class<?> clazz) {
            return new SQLiteSelect(db, clazz);
        }

        public SQLiteSelect select(Class<?>... clazz) {
            return new SQLiteSelect(db, clazz);
        }

        public SQLiteUpdate update(Class<?> clazz) {
            return new SQLiteUpdate(clazz, db);
        }

        public SQLiteDelete delete(Class<?> clazz) {
            return new SQLiteDelete(clazz, db);
        }

        public SQLiteInsert insert(Object entity) {
            return new SQLiteInsert().insert(entity, this.db);
        }
    }
}
