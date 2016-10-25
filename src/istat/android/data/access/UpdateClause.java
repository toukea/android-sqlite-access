package istat.android.data.access;

import istat.android.data.access.interfaces.Queryable;
import android.database.sqlite.SQLiteDatabase;

public class UpdateClause {
	Updater updater;
	DOEntity setEntity;

	protected UpdateClause(Class<? extends Queryable> clazz) {
		updater = new Updater(clazz);
	}

	protected UpdateClause(String table) {
		updater = new Updater(table);
	}

	public Updater setAs(Queryable entity) {
		return new Updater(entity.getEntityName());
	}

	public UpdateClause set(String name, Object value) {
		setEntity.set(name, value);
		return this;
	}

	public Updater where(String column) {
		updater.setEntity(setEntity);
		return updater;
	}

	public class Updater extends BaseClause<Updater> {
		protected Queryable entity;

		protected Updater(Queryable entity) {
			super(entity.getClass());
			// TODO Auto-generated constructor stub
			this.entity = entity;
		}

		private void setEntity(Queryable entity) {
			this.entity = entity;
		}

		protected Updater(Class<? extends Queryable> clazz) {
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
