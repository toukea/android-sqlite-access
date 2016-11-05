package istat.android.data.access;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;

public final class SQLite {
    static Context instanceContext;
    static SQLiteDatabase lastOpenedDb;

    public static SQLiteDatabase getLastOpenedDb() {
        return lastOpenedDb;
    }

    public static SQLiteStatement from(SQLiteDatabase db) {
        lastOpenedDb = db;
        return new SQLiteStatement(db);
    }


    public static SQLiteStatement from(Context context) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQLiteStatement fromDbPath(Context context, String dbPath) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQLiteStatement fromDbFile(Context context, File dbFile) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQLiteStatement fromDbUri(Context context, Uri dbUri) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQLiteDataAccess boot(Context context, String dbName, int dbVersion, final BootDescription description) {
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
        return dAccess;
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
