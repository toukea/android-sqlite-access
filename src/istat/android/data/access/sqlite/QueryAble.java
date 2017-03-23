package istat.android.data.access.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

interface QueryAble {

    long persist(SQLiteDatabase db);

    int delete(SQLiteDatabase db);

    boolean exist(SQLiteDatabase db);

    ContentValues toContentValues();

    void fillFromCursor(Cursor c);

    String getName();

    String[] getColumns();

    String getPrimaryFieldName();

}
