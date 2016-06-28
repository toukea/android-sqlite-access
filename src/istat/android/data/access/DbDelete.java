package istat.android.data.access;

import istat.android.data.access.interfaces.Queryable;
import android.database.sqlite.SQLiteDatabase;

public class DbDelete extends DbClause<DbDelete> {
	public DbDelete(String table) {
		super(table,null);
		// TODO Auto-generated constructor stub
	}

	public DbDelete(Class<? extends Queryable> clazz) {
		super(clazz);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Integer onExecute(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		return db.delete(table, getWhereClose(), getWhereParams());
	}

	public int execute(SQLiteDatabase db) {
		return onExecute(db);
	}

}
