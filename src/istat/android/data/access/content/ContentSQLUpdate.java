package istat.android.data.access.content;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.Iterator;
import java.util.Map;

import istat.android.data.access.content.interfaces.ContentSQLClauseAble;
import istat.android.data.access.content.utils.ContentSQLThread;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;

public final class ContentSQLUpdate implements ContentSQLClauseAble {
    Updater updater;

    ContentSQLUpdate(Class<?> clazz, ContentSQL.SQL sql) {
        updater = new Updater(clazz, sql);
    }

    public Updater setAs(Object entity) {
        try {
            ContentSQLModel model = ContentSQLModel.fromObject(entity);
            updater.model.fieldNameValuePair.putAll(model.fieldNameValuePair);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updater;
    }

    public Updater set(String name, Object value) {
        updater.model.set(name, value);
        return updater;
    }

    public ContentSQLClause.ClauseBuilder where(String column) {
        return updater.where(column);
    }

    public Updater where1() {
        return updater;
    }


    public class Updater extends ContentSQLClause<Updater> {
        protected ContentSQLModel model;

        private void setModel(ContentSQLModel model) {
            this.model = model;
        }

        protected Updater(Class<?> clazz, ContentSQL.SQL sql) {
            super(clazz, sql);
            try {
                model = ContentSQLModel.fromClass(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        protected Updater(Class<?> clazz, String tableName, ContentSQL.SQL sql) {
//            super(clazz, tableName, null, sql);
//        }

        public Updater set(String name, Object value) {
            model.set(name, value);
            return this;
        }

        @Override
        protected Object onExecute(SQLiteDatabase db) {
            notifyExecuting();
            if (!TextUtils.isEmpty(this.limit)) {
                this.whereClause.append(" LIMIT " + limit);
            }
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

        public ContentSQLThread<Integer> executeAsync() {
            return executeAsync(-1, null);
        }

        public ContentSQLThread<Integer> executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<Integer> callback) {
            return executeAsync(-1, callback);
        }

        public ContentSQLThread<Integer> executeAsync(final int limit, final ContentSQLAsyncExecutor.ExecutionCallback<Integer> callback) {
            ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
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

        private int execute(String limit) {
            this.limit = limit;
            return execute();
        }


        @Override
        public String getStatement() {
            String out = "UPDATE FROM " + table;
            ContentValues contentValues = model.toContentValues();
            if (contentValues != null && contentValues.size() > 0) {
                out += " SET ";
                Iterator<Map.Entry<String, Object>> valueSetIterator = contentValues.valueSet().iterator();
                while (valueSetIterator.hasNext()) {
                    Map.Entry<String, Object> set = valueSetIterator.next();
                    out += set.getKey() + " = " + set.getValue();
                    if (valueSetIterator.hasNext()) {
                        out += ", ";
                    }
                }
            }
            String whereClause = getWhereClause();
            String limit = getLimit();
            if (!TextUtils.isEmpty(whereClause)) {
                out += " WHERE " + whereClause.trim();
            }
            String sql = compute(out, this.whereParams);
            if (!TextUtils.isEmpty(limit)) {
                sql += " LIMIT " + limit;
            }
            return sql;
        }

        public SQLiteUpdateLimit limit(int limit) {
            return limit(-1, limit);
        }

        public SQLiteUpdateLimit limit(int offset, int limit) {
            String limitS;
            if (limit < 0) {
                limitS = null;
            } else {
                if (offset < 0) {
                    offset = 0;
                }
                limitS = offset + ", " + limit;
            }
            return new SQLiteUpdateLimit(this, limitS);
        }
    }

    @Override
    public ContentSQL.SQL getInternalSQL() {
        return this.updater.getInternalSQL();
    }

    public class SQLiteUpdateLimit {
        Updater updater;

        SQLiteUpdateLimit(Updater updater, String limitS) {
            this.updater = updater;
            this.updater.limit = limitS;
            this.updater = updater;
        }

        public int execute() {
            return this.updater.execute();
        }

        public int execute(int limit) {
            return this.updater.execute(limit);
        }

        public ContentSQLThread<Integer> executeAsync() {
            return this.updater.executeAsync();
        }

        public ContentSQLThread<Integer> executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<Integer> callback) {
            return this.updater.executeAsync(callback);
        }

        public ContentSQLThread<Integer> executeAsync(final int limit, final ContentSQLAsyncExecutor.ExecutionCallback<Integer> callback) {
            return this.updater.executeAsync(limit, callback);
        }

        public String getStatement() {
            return this.updater.getStatement();
        }
    }
}
