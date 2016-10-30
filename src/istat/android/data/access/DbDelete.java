package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class DbDelete extends DbClause<DbDelete> {
	public DbDelete(String table) {
		super(table,null);
	}

	public DbDelete(Class<? extends QueryAble> clazz) {
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
