package istat.android.data.access;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;

public final class SQLite {
    static Context instanceContext;

    public static SQLiteStatement from(SQLiteDatabase db) {
        return new SQLiteStatement(db);
    }

    public static SQLiteStatement fromDbName(Context context, String dbName) {
        return from(null);
    }

    public static SQLiteStatement fromDbPath(Context context, String dbPath) {
        return from(null);
    }

    public static SQLiteStatement fromDbFile(Context context, File dbFile) {
        return from(null);
    }

    public static SQLiteStatement fromDbUri(Context context, Uri dbUri) {
        return from(null);
    }

    public static SQLiteDatabase boot(Context context, String dbName, int dbVersion, final BootDescription description) {
        instanceContext = context;
        SQLiteDataAccess dAccess = new SQLiteDataAccess(context, dbName, dbVersion) {
            @Override
            public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (description != null) {
                    description.onDbUpgrade(db, oldVersion, newVersion);
                }
            }

            @Override
            public void onDbCreate(SQLiteDatabase db) {
                if (description != null) {
                    description.onDbCreate(db);
                }
            }
        };
        return null;
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

    public static interface BootDescription {
        abstract void onDbUpgrade(SQLiteDatabase db, int oldVersion,
                                  int newVersion);

        abstract void onDbCreate(SQLiteDatabase db);
    }
}
