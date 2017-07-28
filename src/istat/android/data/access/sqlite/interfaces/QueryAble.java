package istat.android.data.access.sqlite.interfaces;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public interface QueryAble {

    long insert(SQLiteDatabase db) throws SQLiteException;

    int delete(SQLiteDatabase db);

    boolean exist(SQLiteDatabase db);

    ContentValues toContentValues();

    void fillFromCursor(Cursor c);

    String getName();

    String[] getColumns();

    String getPrimaryKeyName();

}
