package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;
import istat.android.data.access.interfaces.Queryable;

public class SQL {
	public static DbSelection select(Class<? extends Queryable> clazz) {
		return new DbSelection(clazz);
	}

	public static DbUpdate update(Class<? extends Queryable> clazz) {
		return new DbUpdate(clazz);
	}

	public static DbDelete delete(Class<? extends Queryable> clazz) {
		return new DbDelete(clazz);
	}

	public static DbInsert insert(Queryable entity) {
		return new DbInsert().insert(entity);
	}

	protected static void demo(Queryable entity, SQLiteDatabase db) {
		SQL.insert(entity).insert(entity).execute(db);
		SQL.update(Queryable.class).setAs(null).where("name").equal(2)
				.and("lastname").equal("mama").execute(db);
	}

	public static class SELECT {
		public static DbSelection from(Class<? extends Queryable> clazz) {
			return new DbSelection(clazz);
		}
	}

	public static class UPDATE {
		public static DbUpdate table(Class<? extends Queryable> clazz) {
			return new DbUpdate(clazz);
		}
	}

	public static class DELETE {
		public static DbDelete from(Class<? extends Queryable> clazz) {
			return new DbDelete(clazz);
		}
	}

	public static class INSERT {
		public static DbInsert entity(Queryable entity) {
			return new DbInsert().insert(entity);
		}
	}
}
