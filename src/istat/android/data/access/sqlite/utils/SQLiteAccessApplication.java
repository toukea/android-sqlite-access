package istat.android.data.access.sqlite.utils;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import istat.android.data.access.sqlite.SQLite;
import istat.android.data.access.sqlite.SQLiteDelete;
import istat.android.data.access.sqlite.SQLiteInsert;
import istat.android.data.access.sqlite.SQLiteMerge;
import istat.android.data.access.sqlite.SQLitePersist;
import istat.android.data.access.sqlite.SQLiteSelect;
import istat.android.data.access.sqlite.SQLiteUpdate;

/**
 * Created by istat on 10/11/16.
 */

public abstract class SQLiteAccessApplication extends Application implements SQLite.BootDescription {
    static String applicationDbName;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationDbName = getDbName();
        SQLite.connect(this, applicationDbName, getDbVersion(), this);
    }

    protected abstract String getDbName();

    protected abstract int getDbVersion();

    protected final static void executeScripts(SQLiteDatabase db, List<String> scripts) {
        for (String script : scripts) {
            db.execSQL(script);
        }
    }

    public static SQLiteSelect select(Class<?>... cLass) throws Exception {
        return SQLite.fromConnection(applicationDbName, true).select(cLass);
    }

    public static SQLiteInsert insert(Object... obj) throws Exception {
        return SQLite.fromConnection(applicationDbName, true).insert(obj);
    }

    public static SQLiteMerge merge(Object... obj) throws Exception {
        return SQLite.fromConnection(applicationDbName, true).merge(obj);
    }

    public static SQLitePersist persist(Object... obj) throws Exception {
        return SQLite.fromConnection(applicationDbName, true).persist(obj);
    }

    public static SQLiteUpdate update(Class<?> cLass) throws Exception {
        return SQLite.fromConnection(applicationDbName, true).update(cLass);
    }

    public static SQLiteDelete delete(Class<?> cLass) throws Exception {
        return SQLite.fromConnection(applicationDbName, true).delete(cLass);
    }
}
