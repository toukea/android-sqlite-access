package istat.android.data.access.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import istat.android.data.access.sqlite.utils.SQLiteParser;

public final class SQLite {
    static SQLiteDatabase
            lastOpenedDb;
    final static ConcurrentHashMap<String, SQLiteDataAccess> dbNameAccessPair = new ConcurrentHashMap<String, SQLiteDataAccess>();
    final static ConcurrentHashMap<String, SQLiteConnection> dbNameConnectionPair = new ConcurrentHashMap<String, SQLiteConnection>();

    private SQLite() {

    }

    public static SQLiteDatabase getLastOpenedDb() {
        return lastOpenedDb;
    }

    public static SQL from(SQLiteDatabase db) {
        lastOpenedDb = db;
        return new SQL(db);
    }

    public static SQL fromConnection(String dbName) throws Exception {
        SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);
        return SQLite.from(access.open());
    }

    public static void addConnection(SQLiteConnection... connections) {
        for (SQLiteConnection launcher : connections) {
            addConnection(launcher, false);
        }
    }

    public static SQLiteDataAccess getAccess(String dbName) {
        try {
            return findOrCreateConnectionAccess(dbName);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SQLiteDatabase getDataBase(String dbName) {
        try {
            SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);
            if (access != null) {
                return access.open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void addConnection(SQLiteConnection connection, boolean connectInstantly) {
        if (connectInstantly) {
            connect(connection);
        } else {
            dbNameConnectionPair.put(connection.dbName, connection);
        }
    }

    public static boolean removeConnection(String dbName) {
        boolean contain = dbNameAccessPair.containsKey(dbName);
        if (contain) {
            dbNameAccessPair.remove(dbName);
        }
        return contain;
    }

    public static boolean removeConnection(SQLiteConnection boot) {
        return removeConnection(boot.dbName);
    }

    public static void prepareSQL(String dbName, PrepareHandler handler) {
        prepareSQL(dbName, handler, false);
    }

    public static void prepareTransactionalSQL(String dbName, PrepareHandler handler) {
        prepareSQL(dbName, handler, true);
    }

    private static SQLiteDataAccess findOrCreateConnectionAccess(String dbName) throws IllegalAccessException {
        SQLiteDataAccess access = dbNameAccessPair.get(dbName);
        boolean hasLauncher = dbNameConnectionPair.containsKey(dbName);
        if (access == null && hasLauncher) {
            access = connect(dbNameConnectionPair.get(dbName));
            dbNameConnectionPair.remove(dbName);
        } else {
            throw new IllegalAccessException("Oups, no launcher is currently added dor Data base with name: " + dbName);
        }
        return access;
    }

    public static void prepareSQL(String dbName, PrepareHandler handler, boolean transactional) {
        SQLiteDatabase db = null;
        try {
            SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);
            db = access.open();
            SQL sql = SQLite.from(db);
            handler.onSQLReady(sql);
            if (transactional) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            handler.onSQLPrepareFail(e);

        } finally {
            if (transactional && db != null) {
                db.endTransaction();
                if (db.isOpen()) {
                    db.close();
                }
            }

        }
    }

    public static void prepareSQL(SQLiteConnection connection, PrepareHandler handler) {
        prepareSQL(connection, false, handler);
    }

    public static void prepareTransactionalSQL(SQLiteConnection connection, PrepareHandler handler) {
        prepareSQL(connection, true, handler);
    }

    public static void prepareSQL(SQLiteConnection boot, boolean transactional, PrepareHandler handler) {
        SQLiteDatabase db = null;
        try {
            SQLiteDataAccess access = connect(boot);
            db = access.open();
            if (transactional) {
                db.beginTransaction();
            }
            SQL sql = SQLite.from(db);
            handler.onSQLReady(sql);
            if (db.isOpen()) {
                db.close();
            }
            if (transactional) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            handler.onSQLPrepareFail(e);
        } finally {
            if (transactional && db != null) {
                db.endTransaction();
                if (db.isOpen()) {
                    db.close();
                }
            }
        }
    }

    public static void prepareSQL(SQLiteDatabase db, PrepareHandler handler) {
        prepareSQL(db, false, handler);
    }

    public static void prepareSQL(SQLiteDatabase db, boolean transactional, PrepareHandler handler) {
        try {
            if (transactional) {
                db.beginTransaction();
            }
            SQL sql = SQLite.from(db);
            handler.onSQLReady(sql);

            if (transactional) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (transactional && db != null) {
                db.endTransaction();
                if (db.isOpen()) {
                    db.close();
                }
            }
        }
    }

//    public static SQL from(String dbName) throws IllegalAccessException {
//        SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);
//        SQLiteDatabase db = access.open();
//        return from(db);
//    }
//
//    @Deprecated
//    public static SQL from(Context context, SQLiteConnection boot) {
//        SQLiteDatabase db = connect(boot).open();
//        return from(db);
//    }


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

    public static SQLiteDataAccess connect(SQLiteConnection boot) {
        return connect(boot.context, boot.dbName, boot.dbVersion, boot);
    }

    public static void close(String dbName) {
        SQLiteDataAccess access = dbNameAccessPair.get(dbName);
        if (access != null) {
            access.close();
        }
    }

    public static void desconnect(String dbName) {
        SQLiteDataAccess access = dbNameAccessPair.get(dbName);
        if (access != null) {
            access.close();
            dbNameAccessPair.remove(dbName);
        }
    }

    public static SQLiteDataAccess connect(Context context, String dbName, int dbVersion, final BootDescription description) {
        SQLiteDataAccess access = new SQLiteDataAccess(context, dbName, dbVersion) {
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
        dbNameAccessPair.put(dbName, access);
        return access;
    }


    public static class SQL {
        SQLiteDatabase db;

        SQL(SQLiteDatabase db) {
            this.db = db;
        }

        public SQLiteSelect select(Class<?> clazz) {
            return new SQLiteSelect(this, clazz);
        }

        public SQLiteSelect select(Class<?>... clazz) {
            return new SQLiteSelect(this, clazz);
        }

        public SQLiteUpdate update(Class<?> clazz) {
            return new SQLiteUpdate(clazz, this);
        }

        public SQLiteDelete delete(Class<?> clazz) {
            return new SQLiteDelete(clazz, this);
        }

        public SQLiteInsert insert(Object entity) {
            SQLiteInsert insert = new SQLiteInsert(this);
            return insert.insert(entity);
        }

        public SQLiteInsert insert(Object... entity) {
            SQLiteInsert insert = new SQLiteInsert(this);
            return insert.insert(entity);
        }

        public <T> SQLiteInsert insert(List<T> entity) {
            SQLiteInsert insert = new SQLiteInsert(this);
            return insert.insert(entity);
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

        public final void beginTransaction() {
            db.beginTransaction();
        }

        public final void setTransactionSuccessful() {
            db.setTransactionSuccessful();
        }

        public final void endTransaction() {
            db.endTransaction();
        }


    }

    public static abstract class SQLiteConnection implements BootDescription {
        String dbName;
        int dbVersion = 1;
        Context context;

        public SQLiteConnection(Context context, String dbName, int dbVersion) {
            this.dbName = dbName;
            this.dbVersion = dbVersion;
            this.context = context;
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

    public interface PrepareHandler {
        public void onSQLReady(SQL sql);

        public void onSQLPrepareFail(Exception e);
    }

    public static abstract class SQLReadyHandler implements PrepareHandler {

        public void onSQLPrepareFail(Exception e) {
            e.printStackTrace();
        }
    }
}
