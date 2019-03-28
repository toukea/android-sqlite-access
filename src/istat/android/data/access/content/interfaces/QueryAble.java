package istat.android.data.access.content.interfaces;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public interface QueryAble {

    long insert(ContentResolver db) throws SQLiteException;

    int delete(ContentResolver db);

    boolean exist(ContentResolver db);

    ContentValues toContentValues();

    void fillFromCursor(Cursor c);

    String getName();

    String[] getColumns();

    String getPrimaryKeyName();

}
