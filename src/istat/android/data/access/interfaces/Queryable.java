package istat.android.data.access.interfaces;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract interface Queryable {
	public abstract ContentValues toContentValues();

	abstract void fillFromCursor(Cursor c);

	public long persist(SQLiteDatabase db);

	public long insert(SQLiteDatabase db);

	abstract int update(SQLiteDatabase db);

	abstract int delete(SQLiteDatabase db);

	public abstract boolean exist(SQLiteDatabase db);

	public abstract String getEntityName();

	public abstract String[] getEntityFieldNames();

	public abstract String getEntityPrimaryFieldName();

}
