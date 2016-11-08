package istat.android.data.access.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import istat.android.data.access.sqlite.utils.SQLiteParser;

public final class SQLite {
    static Context instanceContext;
    static SQLiteDatabase lastOpenedDb;

    public static SQLiteDatabase getLastOpenedDb() {
        return lastOpenedDb;
    }

    public static SQL from(SQLiteDatabase db) {
        lastOpenedDb = db;
        return new SQL(db);
    }

    public static void from(SQLiteDatabase db, SQLExecutor executor) {
        SQL sql = SQLite.from(db);
        executor.onExecute(sql, db);
        try {
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SQL from(Context context, String dbName, int dbVersion, BootDescription description) {
        SQLiteDatabase db = boot(context, dbName, dbVersion, description).open();
        return from(db);
    }

    public static SQL from(Context context, SQLiteBoot boot) {
        SQLiteDatabase db = boot(context, boot).open();
        return from(db);
    }


    public static SQL fromDbPath(Context context, String dbPath) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQL fromDbFile(Context context, File dbFile) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQL fromDbUri(Context context, Uri dbUri) {
        SQLiteDatabase db = null;
        return from(db);
    }

    public static SQLiteDataAccess boot(Context context, SQLiteBoot boot) {
        return boot(context, boot.dbName, boot.dbVersion, boot);
    }

    public static SQLiteDataAccess boot(Context context, String dbName, int dbVersion, final BootDescription description) {
        instanceContext = context;
        SQLiteDataAccess dAccess = new SQLiteDataAccess(context, dbName, dbVersion) {
            @Override
            public void onUpgradeDb(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (description != null) {
                    description.onUpgradeDb(db, oldVersion, newVersion);
                }
            }

            @Override
            public void onCreateDb(SQLiteDatabase db) {
                if (description != null) {
                    description.onCreateDb(db);
                }
            }
        };
        return dAccess;
    }


    public static class SQL {
        SQLiteDatabase db;

        SQL(SQLiteDatabase db) {
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

        public void executeStatements(List<String> statements) {
            for (String ask : statements) {
                db.execSQL(ask);
            }
        }

        public void executeStatements(String... statements) {
            for (String ask : statements) {
                db.execSQL(ask);
            }
        }

        public void executeSQLScript(InputStream sqlFileInputStream) throws IOException {
            List<String> statements = SQLiteParser.parseSqlFile(sqlFileInputStream);
            for (String statement : statements) {
                db.execSQL(statement);
            }
        }

        public final void closeDb() {
            db.close();
        }


    }

    public static abstract class SQLiteBoot implements BootDescription {
        String dbName;
        int dbVersion = 1;

        public SQLiteBoot(String dbName, int dbVersion) {
            this.dbName = dbName;
            this.dbVersion = dbVersion;
        }
    }

    public static interface BootDescription {
        abstract void onCreateDb(SQLiteDatabase db);

        abstract void onUpgradeDb(SQLiteDatabase db, int oldVersion,
                                  int newVersion);
    }

    public static void executeSQLScript(SQLiteDatabase db,
                                        InputStream sqlFileInputStream) throws IOException {
        List<String> statements = SQLiteParser.parseSqlFile(sqlFileInputStream);
        for (String statement : statements)
            db.execSQL(statement);
    }

    public interface SQLExecutor {
        public void onExecute(SQL sql, SQLiteDatabase db);
    }
}
