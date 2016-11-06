package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;
import istat.android.data.access.interfaces.Queryable;

public class SQL {
	public static SelectClause select(Class<? extends Queryable> clazz) {
		return new SelectClause(clazz);
	}

	public static UpdateClause update(Class<? extends Queryable> clazz) {
		return new UpdateClause(clazz);
	}

	public static DeleteClause delete(Class<? extends Queryable> clazz) {
		return new DeleteClause(clazz);
	}

	public static InsertClause insert(Queryable entity) {
		return new InsertClause().insert(entity);
	}

	protected static void demo(Queryable entity, SQLiteDatabase db) {
		SQL.insert(entity).insert(entity).execute(db);
		SQL.update(Queryable.class).setAs(null).where("name").equal(2)
				.and("lastname").equal("mama").execute(db);
	}

	public static class SELECT {
		public static SelectClause from(Class<? extends Queryable> clazz) {
			return new SelectClause(clazz);
		}
	}

	public static class UPDATE {
		public static UpdateClause table(Class<? extends Queryable> clazz) {
			return new UpdateClause(clazz);
		}
	}

	public static class DELETE {
		public static DeleteClause from(Class<? extends Queryable> clazz) {
			return new DeleteClause(clazz);
		}
	}

	public static class INSERT {
		public static InsertClause entity(Queryable entity) {
			return new InsertClause().insert(entity);
		}
	}
}
