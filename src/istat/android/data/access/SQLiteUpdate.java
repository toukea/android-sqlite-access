package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class SQLiteUpdate {
	Updater updater;
	SQLiteModel setEntity;

	protected SQLiteUpdate(Class<? extends QueryAble> clazz) {
		updater = new Updater(clazz);
	}

	protected SQLiteUpdate(String table) {
		updater = new Updater(table);
	}

	public Updater setAs(QueryAble entity) {
		return new Updater(entity.getEntityName());
	}

	public SQLiteUpdate set(String name, Object value) {
		setEntity.set(name, value);
		return this;
	}

	public Updater where(String column) {
		updater.setEntity(setEntity);
		return updater;
	}

	public class Updater extends SQLiteClause<Updater> {
		protected QueryAble entity;

		protected Updater(QueryAble entity) {
			super(entity.getClass());
			// TODO Auto-generated constructor stub
			this.entity = entity;
		}

		private void setEntity(QueryAble entity) {
			this.entity = entity;
		}

		protected Updater(Class<? extends QueryAble> clazz) {
			super(clazz);
			// TODO Auto-generated constructor stub
		}

		protected Updater(String clazz) {
			super(clazz, null);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected Object onExecute(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			return db.update(entity.getEntityName(), entity.toContentValues(),
					getWhereClose(), getWhereParams());
		}

		public int execute(SQLiteDatabase db) {
			return Integer.valueOf(onExecute(db) + "");
		}

		@Override
		public final String getSQL() {
			// TODO Auto-generated method stub
			return super.getSQL();
		}
	}
}
