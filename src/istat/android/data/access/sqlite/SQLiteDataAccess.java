package istat.android.data.access.sqlite;

import istat.android.data.access.sqlite.utils.SQLiteParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

/*
 * Copyright (C) 2014 Istat Dev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Toukea Tatsi (Istat)
 */
public class SQLiteDataAccess implements Closeable, Cloneable {

    private static final String DB_CURRENT_VERSION = "db_last_known_version";
    /*
         * protected static final int BASE_VERSION = 1; protected static final
         * String BASE_NOM = "istatLib.db";
         */
    // L�instance de la base qui sera manipul�e au travers de cette classe.
    protected SQLiteDatabase db;
    private DbOpenHelper dbOpenHelper;
    public Context context;
    protected final static String SHARED_PREF_FILE = "db_file",
            DB_CREATION_TIME = "creation_time",
            DB_UPDATE_TIME = "creation_time";
    final String dbName;
    final int dbVersion;

//    protected SQLiteDataAccess(SQLiteDataAccess accessModel) {
//        this(accessModel.getContext(), accessModel.getDbName(), accessModel.getDbVersion(), accessModel.getBootDescription());
//    }

    protected SQLiteDataAccess(Context ctx, String dbName, int dbVersion, SQLite.BootDescription bootDescription) {
        dbOpenHelper = new DbOpenHelper(this, dbName, null, dbVersion, bootDescription);
        context = ctx;
        this.dbName = dbName;
        this.dbVersion = dbVersion;
    }

    public SQLite.BootDescription getBootDescription() {
        return dbOpenHelper.bootDescription;
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public String getDbName() {
        return dbName;
    }

    public Context getContext() {
        return context;
    }

    /**
     * Ouvre la base de donnees en ecriture.
     */
    public SQLiteDatabase open() {
        db = dbOpenHelper.getWritableDatabase();
        Log.i("openhelper", "BDD open");
        return db;
    }

    public boolean checkUp() {
        try {
            SQLiteDatabase db = open();
            if (db == null) {
                return false;
            }
            close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean isOpened() {
        return db != null;
    }

    /**
     * Ferme la base de donnees.
     */
    public void close() {
        if (db != null)
            if (db.isOpen()) {
                try {
                    db.close();
                    Log.i("openhelper", "BDD close");
                } catch (Exception e) {
                    Log.i("openhelper",
                            "BDD can't be close because it is not already Open");
                }

            } else {
                Log.i("openhelper",
                        "BDD can't be close because it is not already Open");
            }
        this.db = null;

    }

    public final void beginTransaction() {
        db.beginTransaction();
    }

    public final void endTransaction() {
        db.endTransaction();
    }

    public final void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }

    public final void commit() {
        setTransactionSuccessful();
        endTransaction();
    }

    public final SQLite.SQL getStatement() {
        return SQLite.from(open());
    }

    public int truncateTable(String table) {
        return db.delete(table, null, null);
    }

    // public <T extends SQLiteModel> int truncateTable(Class<T> clazz) {
    // T instance = null;
    // String table = "";
    // try {
    // instance = clazz.newInstance();
    // table = instance.getName();
    // } catch (Exception e) {
    // throw new RuntimeException(
    // "not default constructor found for class::" + clazz);
    // }
    // return db.delete(table, null, null);
    // }


    /**
     * get the current writable Db
     */
    public SQLiteDatabase getDataBase() {
        return db;
    }

    public DbOpenHelper getDbOpenHelper() {
        return dbOpenHelper;
    }

    public boolean doesTableExist(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(
                "select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                        + tableName + "'", null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    private String getNameSpace() {
        return getNameSpace(this.dbName);
    }

    private static String getNameSpace(String dbName) {
        return dbName + SHARED_PREF_FILE;
    }

    public String getDbUpdateTime() {
        return context.getSharedPreferences(getNameSpace(), 0).getString(
                DB_UPDATE_TIME, simpleDateTime());
    }

    public String getDbCreationTime() {
        return context.getSharedPreferences(getNameSpace(), 0).getString(
                DB_CREATION_TIME, simpleDateTime());
    }

    @SuppressWarnings("deprecation")
    public Date getDbUpdateTimeAsDate() {
        return new Date(context.getSharedPreferences(getNameSpace(), 0)
                .getString(DB_UPDATE_TIME, simpleDateTime()));
    }

    @SuppressWarnings("deprecation")
    public Date getDbCreationTimeAsDate() {
        return new Date(context.getSharedPreferences(getNameSpace(), 0)
                .getString(DB_CREATION_TIME, simpleDateTime()));
    }

    protected boolean isTableExist(String table) {
        return isTableExist(db, table);
    }

    // -----------------------------------------------------------------------------------------------------------
    protected boolean executeRawResource(SQLiteDatabase db, int resId) {
        try {
            Resources res = getContext().getResources();
            InputStream resStream = res.openRawResource(resId);
            executeDbScript(db, resStream);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void executeRawRessources(SQLiteDatabase db, int... resourceIds) {
        for (int index : resourceIds) {
            executeRawResource(db, index);
        }
    }

    protected void executeStatements(SQLiteDatabase db, int[] statements) {
        for (int ask : statements)
            db.execSQL(context.getResources().getString(ask));
    }

    protected static void executeStatements(SQLiteDatabase db,
                                            List<String> statements) {
        for (String ask : statements)
            db.execSQL(ask);
    }

    protected static void executeStatements(SQLiteDatabase db,
                                            String[] statements) {
        for (String statement : statements)
            db.execSQL(statement);
    }

    // ----------------------------------------------------------------------------
    static class DbOpenHelper extends SQLiteOpenHelper {
        SQLite.BootDescription bootDescription;
        SQLiteDataAccess access;

        public DbOpenHelper(SQLiteDataAccess access, String nom,
                            CursorFactory cursorfactory, int version, SQLite.BootDescription description) {
            super(access.getContext(), nom, cursorfactory, version);
            this.access = access;
            this.bootDescription = description;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (bootDescription != null) {
                bootDescription.onCreateDb(db);
            }
            this.access.registerDbCreationTime();
//            registerAsLastAccessVersion(db.getVersion());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (bootDescription != null) {
                bootDescription.onUpgradeDb(db, oldVersion, newVersion);
            }
            this.access.registerDbUpdateTime();
//            registerAsLastAccessVersion(newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (bootDescription != null) {
                bootDescription.onOpen(db);
            }
            this.access.registerAsLastAccessVersion(db.getVersion());
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            if (bootDescription != null) {
                bootDescription.onConfigure(db);
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (bootDescription != null) {
                if (bootDescription.onDowngrade(db, oldVersion, newVersion)) {
                    super.onDowngrade(db, oldVersion, newVersion);
                }
            } else {
                super.onDowngrade(db, oldVersion, newVersion);
            }
        }
    }

    public static int getLastAccessDbVersion(Context context, String dbName) {
        return context.getSharedPreferences(getNameSpace(dbName), 0).getInt(
                DB_CURRENT_VERSION, -1);
    }

    private void registerAsLastAccessVersion(int version) {
        SharedPreferences p = context.getSharedPreferences(getNameSpace(), 0);
        SharedPreferences.Editor editor = p.edit();
        editor.putInt(DB_CURRENT_VERSION, version);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private void registerDbCreationTime() {
        SharedPreferences p = context.getSharedPreferences(getNameSpace(), 0);
        SharedPreferences.Editor editor = p.edit();
        editor.putString(DB_CREATION_TIME, simpleDateTime());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private void registerDbUpdateTime() {
        SharedPreferences p = context.getSharedPreferences(getNameSpace(), 0);
        SharedPreferences.Editor editor = p.edit();
        editor.putString(DB_UPDATE_TIME, simpleDateTime());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    // ----------------------------------------------------------------------------
    public static void executeDbScript(SQLiteDatabase db,
                                       InputStream sqlFileInputStream) throws IOException {
        List<String> statements = SQLiteParser.parseSqlStream(sqlFileInputStream);
        for (String statement : statements)
            db.execSQL(statement);
    }

    public static boolean isTableExist(SQLiteDatabase db, String table) {
        try {
            db.query(table, null, null, null, null, null, null);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    @SuppressLint("SimpleDateFormat")
    public static String simpleDateTime() {

        Date date = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return f.format(date);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Object obj = super.clone();
        return obj;
    }

    protected SQLiteDataAccess cloneAccess() throws CloneNotSupportedException {
        Object obj = super.clone();
        SQLiteDataAccess access = (SQLiteDataAccess) obj;
        return access;
    }
}
