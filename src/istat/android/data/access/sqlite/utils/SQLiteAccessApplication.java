package istat.android.data.access.sqlite.utils;

import android.app.Application;

import istat.android.data.access.sqlite.SQLite;

/**
 * Created by istat on 10/11/16.
 */

public abstract class SQLiteAccessApplication extends Application implements SQLite.BootDescription {
    @Override
    public void onCreate() {
        super.onCreate();
        SQLite.connect(this, getDbName(), getDbVersion(), this);
    }

    protected abstract String getDbName();

    protected abstract int getDbVersion();

}
