package istat.android.data.access;

import istat.android.data.access.interfaces.Queryable;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class SelectClause extends BaseClause<SelectClause> {
	Class<? extends Queryable> clazz;
	String join;

	public SelectClause(Class<? extends Queryable> clazz) {
		super(clazz);
		// TODO Auto-generated constructor stub
		this.clazz = clazz;
	}

	public SelectClause(Class<? extends Queryable>... clazz) {
		super(clazz[0]);
		// TODO Auto-generated constructor stub
		this.clazz = clazz[0];
	}

	public SelectClause join(Class<? extends Queryable> clazz, String on) {
		Queryable entity = createEntityInstance(clazz);
		join = entity.getEntityName();
		table += " INNER JOIN " + join;
		if (!TextUtils.isEmpty(on)) {
			table += " ON (" + on + ") ";
		}
		return this;
	}

	@Override
	protected Cursor onExecute(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		return db.query(table, projection, getWhereClose(), getWhereParams(),
				null, null, getOrderBy());

	}

	// public List<Queryable> commit(SQLiteDatabase db) {
	// List<Queryable> list = new ArrayList<Queryable>();
	// Cursor c = execute(db);
	// if (c.getCount() > 0) {
	// while (c.moveToNext()) {
	// list.add(createFromCursor(clazz, c));
	// }
	// }
	// c.close();
	// return list;
	//
	// }
	@SuppressWarnings("unchecked")
	public <T extends Queryable> List<T> execute(SQLiteDatabase db) {
		List<T> list = new ArrayList<T>();
		Cursor c = onExecute(db);
		if (c.getCount() > 0) {
			while (c.moveToNext()) {
				list.add((T) createFromCursor(clazz, c));
			}
		}
		c.close();
		return list;

	}

	public int count(SQLiteDatabase db) {
		Cursor c = onExecute(db);
		int count = c.getCount();
		c.close();
		return count;

	}

	@SuppressWarnings("unchecked")
	public <T extends Queryable> void execute(SQLiteDatabase db, List<T> list) {
		if (list == null)
			list = new ArrayList<T>();
		Cursor c = onExecute(db);
		if (c.getCount() > 0) {
			while (c.moveToNext()) {
				list.add((T) createFromCursor(clazz, c));
			}
		}
		c.close();

	}

	public Clauser AND_SELECT(SelectClause close) {
		this.whereClose = "(SELECT * FROM " + table + " WHERE "
				+ close.whereClose + ")";
		this.whereParams = close.whereParams;
		return new Clauser(TYPE_CLAUSE_AND);
	}

	public Clauser OR_SELECT(SelectClause close) {
		this.whereClose = close.whereClose;
		this.whereParams = close.whereParams;
		return new Clauser(TYPE_CLAUSE_AND);
	}

	@Override
	public final String getSQL() {
		// TODO Auto-generated method stub
		return super.getSQL();
	}

}
