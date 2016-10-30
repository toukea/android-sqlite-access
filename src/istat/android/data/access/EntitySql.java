package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

import istat.android.data.access.interfaces.QueryAble;

public class EntitySql {
	public static DbSelection select(Class<? extends QueryAble> clazz) {
		return new DbSelection(clazz);
	}

	public static DbUpdate update(Class<? extends QueryAble> clazz) {
		return new DbUpdate(clazz);
	}

	public static DbDelete delete(Class<? extends QueryAble> clazz) {
		return new DbDelete(clazz);
	}

	public static DbInsert insert(QueryAble entity) {
		return new DbInsert().insert(entity);
	}

	protected static void demo(QueryAble entity, SQLiteDatabase db) {
		EntitySql.insert(entity).insert(entity).execute(db);
		EntitySql.update(QueryAble.class).setAs(null).where("name").equal(2)
				.and("lastname").equal("mama").execute(db);
	}

	public static class SELECT {
		public static DbSelection from(Class<? extends QueryAble> clazz) {
			return new DbSelection(clazz);
		}
	}

	public static class UPDATE {
		public static DbUpdate table(Class<? extends QueryAble> clazz) {
			return new DbUpdate(clazz);
		}
	}

	public static class DELETE {
		public static DbDelete from(Class<? extends QueryAble> clazz) {
			return new DbDelete(clazz);
		}
	}

	public static class INSERT {
		public static DbInsert entity(QueryAble entity) {
			return new DbInsert().insert(entity);
		}
	}
}
