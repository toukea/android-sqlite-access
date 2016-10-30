package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class SQLiteDelete extends SQLiteClause<SQLiteDelete> {
	public SQLiteDelete(String table) {
		super(table,null);
	}

	public SQLiteDelete(Class<? extends QueryAble> clazz) {
		super(clazz);
	}

	@Override
	protected Integer onExecute(SQLiteDatabase db) {
		return db.delete(table, getWhereClose(), getWhereParams());
	}

	public int execute(SQLiteDatabase db) {
		return onExecute(db);
	}

}
