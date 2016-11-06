package istat.android.data.access;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

abstract interface QueryAble {


    long persist(SQLiteDatabase db);

    abstract int delete(SQLiteDatabase db);

    public abstract boolean exist(SQLiteDatabase db);

    abstract ContentValues toContentValues();

    void fillFromCursor(Cursor c);

    abstract String getName();

    abstract String[] getProjections();

    abstract String getPrimaryFieldName();

}
