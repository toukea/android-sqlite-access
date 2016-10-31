package istat.android.data.access;

import android.database.sqlite.SQLiteDatabase;

public class SQLiteUpdate {
    Updater updater;
    SQLiteModel setEntity;

    SQLiteUpdate(Class<?> clazz, SQLiteDatabase db) {
        updater = new Updater(clazz, db);
    }

    SQLiteUpdate(String table, SQLiteDatabase db) {
        updater = new Updater(table, db);
    }

    public Updater setAs(QueryAble entity) {
        return new Updater(entity.getEntityName(), this.updater.db);
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

        protected Updater(QueryAble entity, SQLiteDatabase db) {
            super(entity.getClass(), db);
            this.entity = entity;
        }

        private void setEntity(QueryAble entity) {
            this.entity = entity;
        }

        protected Updater(Class<?> clazz, SQLiteDatabase db) {
            super(clazz, db);
        }

        protected Updater(String clazz, SQLiteDatabase db) {
            super(clazz, null, db);
        }

        @Override
        protected Object onExecute(SQLiteDatabase db) {
            return db.update(entity.getEntityName(), entity.toContentValues(),
                    getWhereClose(), getWhereParams());
        }

        public int execute(SQLiteDatabase db) {
            return Integer.valueOf(onExecute(db) + "");
        }

        @Override
        public final String getSQL() {
            return super.getSQL();
        }
    }
}
