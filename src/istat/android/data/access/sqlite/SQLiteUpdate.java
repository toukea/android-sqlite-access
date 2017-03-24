package istat.android.data.access.sqlite;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteUpdate {
    Updater updater;

    SQLiteUpdate(Class<?> clazz, SQLite.SQL sql) {
        updater = new Updater(clazz, sql);
    }
//
//    SQLiteUpdate(String table, SQLiteDatabase db) {
//        updater = new Updater(table, db);
//    }

    public Updater setAs(Object entity) {
        String tbName = entity.getClass().getName();
        try {
            SQLiteModel model = SQLiteModel.fromObject(entity);
            updater.model.fieldNameValuePair.putAll(model.fieldNameValuePair);
            tbName = model.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Updater(tbName, this.updater.sql);
    }

    public Updater set(String name, Object value) {
        updater.model.set(name, value);
        return updater;
    }

    public SQLiteClause.ClauseBuilder where(String column) {
        return updater.where(column);
    }

    public class Updater extends SQLiteClause<Updater> {
        protected SQLiteModel model;

        private void setModel(SQLiteModel model) {
            this.model = model;
        }

        protected Updater(Class<?> clazz, SQLite.SQL sql) {
            super(clazz, sql);
            try {
                model = SQLiteModel.fromClass(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected Updater(String clazz, SQLite.SQL sql) {
            super(clazz, null, sql);
        }

        public Updater set(String name, Object value) {
            model.set(name, value);
            return this;
        }

        @Override
        protected Object onExecute(SQLiteDatabase db) {
            String whereClause = getWhereClause();
            String[] whereParams = getWhereParams();
            return db.update(model.getName(), model.toContentValues(),
                    whereClause, whereParams);
        }

        public int execute() {
            int out = Integer.valueOf(onExecute(sql.db) + "");
            notifyExecuted();
            return out;
        }

        public SQLiteThread<Integer> executeAsync() {
            return executeAsync(-1, null);
        }

        public SQLiteThread<Integer> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
            return executeAsync(-1, callback);
        }

        public SQLiteThread<Integer> executeAsync(final int limit, final SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
            SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
            return asyncExecutor.execute(this, limit, callback);
        }

        public int execute(int limit) {
            String limitS;
            if (limit < 0) {
                limitS = null;
            } else {
                limitS = "" + limit;
            }
            return execute(limitS);
        }

        public int execute(String limit) {
            this.limit = limit;
            return execute();
        }


        @Override
        public final String getStatement() {
            return super.getStatement();
        }
    }
}
