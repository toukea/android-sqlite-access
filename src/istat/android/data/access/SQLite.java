package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class SQLite {
    public static ClauseProvider from(SQLiteDatabase db) {
        return new ClauseProvider(db);
    }

    public static class ClauseProvider {
        SQLiteDatabase db;

        ClauseProvider(SQLiteDatabase db) {
            this.db = db;
        }

        public SQLiteSelect select(Class<?> clazz) {
            return null;//new SQLiteSelect(clazz);
        }

        public SQLiteUpdate update(Class<?> clazz) {
            return null;//new SQLiteUpdate(clazz);
        }

        public SQLiteDelete delete(Class<?> clazz) {
            return null;// new SQLiteDelete(clazz);
        }

        public SQLiteInsert insert(Object entity) {
            return new SQLiteInsert().insert(entity, this.db);
        }
    }
}
