package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import istat.android.data.access.sqlite.interfaces.SQLiteClauseAble;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;
//TODO insérer une notion de persistance stratégie
public final class SQLitePersist implements SQLiteClauseAble {
    List<SQLiteModel> modelPersist = new ArrayList<SQLiteModel>();
    List<Object> persists = new ArrayList<Object>();
    SQLite.SQL sql;
    Function<SQLiteModel, Boolean> sqliteModelExistCheckFunction;
    ConflictStrategy conflictStrategy;

    SQLitePersist(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLitePersist setSqliteModelExistCheckFunction(Function<SQLiteModel, Boolean> sqliteModelExistCheckFunction) {
        this.sqliteModelExistCheckFunction = sqliteModelExistCheckFunction;
        return this;
    }

    public SQLitePersist setConflictStrategy(ConflictStrategy conflictStrategy) {
        this.conflictStrategy = conflictStrategy;
        return this;
    }

    public SQLitePersist persist(Object insert) {
        try {
            SQLiteModel model = SQLiteModel.fromObject(insert,
                    sql.getSerializer(insert.getClass()),
                    sql.getContentValueHandler(insert.getClass())
            );
            model.setExistCheckFunction(sqliteModelExistCheckFunction);
            model.setConflictStrategy(conflictStrategy);
            modelPersist.add(model);
            persists.add(insert);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLitePersist persist(List<?> insert) {
        for (Object obj : insert) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,
                        sql.getSerializer(insert.getClass()),
                        sql.getContentValueHandler(insert.getClass())
                );
                model.setExistCheckFunction(sqliteModelExistCheckFunction);
                model.setConflictStrategy(conflictStrategy);
                modelPersist.add(model);
                persists.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLitePersist persist(Object... insert) {
        for (Object obj : insert) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,
                        sql.getSerializer(insert.getClass()),
                        sql.getContentValueHandler(insert.getClass())
                );
                model.setExistCheckFunction(sqliteModelExistCheckFunction);
                model.setConflictStrategy(conflictStrategy);
                modelPersist.add(model);
                persists.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute() throws SQLiteException {
        if (modelPersist == null || modelPersist.isEmpty()) {
            return new long[]{0};
        }
        long[] out = new long[modelPersist.size()];
        try {
            int index = 0;
            Object entity;
            for (SQLiteModel insertion : modelPersist) {
                long value = insertion.persist(sql.db);
                entity = persists.get(index);
                if (value > 0) {
                    //TODO update entity to match with new Id state.
                    insertion.flowInto(entity, insertion.getPrimaryKeyName());
                }
                out[index] = value;
                index++;
            }
            modelPersist.clear();
            notifyExecuted();
        } catch (Exception e) {
            SQLiteException error = new SQLiteException(e.getMessage());
            error.initCause(e);
            error.setStackTrace(e.getStackTrace());
            throw error;
        }
        return out;
    }

//    public List<Object> execute() {
//        List<Object> entities = new ArrayList<>();
//        if (modelPersist == null || modelPersist.size() == 0)
//            return entities;
//        int index = 0;
//        for (SQLiteModel insertion : modelPersist) {
//            long out = insertion.persist(sql.db);
//            Object entity = persists.get(index);
//            if (out > 0) {
//                //TODO update entity to match with new Id state.
//                insertion.flowInto(entity, insertion.getPrimaryKeyName());
//            }
//            entities.add(entity);
//            index++;
//        }
//        modelPersist.clear();
//        notifyExecuted();
//        return entities;
//    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }

    @Override
    public SQLite.SQL getInternalSQL() {
        return this.sql;
    }

    public List<Object> getPersists() {
        return persists;
    }

    public SQLiteThread executeAsync() {
        return executeAsync(null);
    }

    public SQLiteThread executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<long[]> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

//    public SQLiteThread executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<List<Object>> callback) {
//        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
//        return asyncExecutor.execute(this, callback);
//    }
    
    public enum ConflictStrategy {
        
        CONFLICT_ROLLBACK(1),

        /**
         * When a constraint violation occurs,no ROLLBACK is executed
         * so changes from prior commands within the same transaction
         * are preserved. This is the default behavior.
         */
        CONFLICT_ABORT(2),

        /**
         * When a constraint violation occurs, the command aborts with a return
         * code SQLITE_CONSTRAINT. But any changes to the database that
         * the command made prior to encountering the constraint violation
         * are preserved and are not backed out.
         */
        CONFLICT_FAIL(3),

        /**
         * When a constraint violation occurs, the one row that contains
         * the constraint violation is not inserted or changed.
         * But the command continues executing normally. Other rows before and
         * after the row that contained the constraint violation continue to be
         * inserted or updated normally. No error is returned.
         */
        CONFLICT_IGNORE(4),

        /**
         * When a UNIQUE constraint violation occurs, the pre-existing rows that
         * are causing the constraint violation are removed prior to inserting
         * or updating the current row. Thus the insert or update always occurs.
         * The command continues executing normally. No error is returned.
         * If a NOT NULL constraint violation occurs, the NULL value is replaced
         * by the default value for that column. If the column has no default
         * value, then the ABORT algorithm is used. If a CHECK constraint
         * violation occurs then the IGNORE algorithm is used. When this conflict
         * resolution strategy deletes rows in order to satisfy a constraint,
         * it does not invoke delete triggers on those rows.
         * This behavior might change in a future release.
         */
        CONFLICT_REPLACE(5),

        /**
         * Use the following when no conflict action is specified.
         */
        CONFLICT_NONE(0);
        final int code;
        ConflictStrategy(int code) {
            this.code = code;
        }
        
    }
}
