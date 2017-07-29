package istat.android.data.access.sqlite.utils;

import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;


import java.util.List;

import istat.android.data.access.sqlite.SQLiteDelete;
import istat.android.data.access.sqlite.SQLiteInsert;
import istat.android.data.access.sqlite.SQLiteMerge;
import istat.android.data.access.sqlite.SQLitePersist;
import istat.android.data.access.sqlite.SQLiteSelect;
import istat.android.data.access.sqlite.SQLiteUpdate;

/**
 * Created by istat on 08/02/17.
 */

public final class SQLiteAsyncExecutor {
    private Handler handler = new Handler(Looper.getMainLooper());
    boolean transactional = true;

    public SQLiteAsyncExecutor(Handler handler) {
        this(handler, true);
    }

    public SQLiteAsyncExecutor(Handler handler, boolean transactional) {
        this.handler = handler;
        this.transactional = transactional;
    }

    public Handler getHandler() {
        return handler;
    }

    public SQLiteAsyncExecutor() {
        this(true);
    }

    public SQLiteAsyncExecutor(boolean transactional) {
        this.transactional = transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, int limit, final SelectionCallback<T> callback) {
        return execute(clause, -1, limit, callback);
    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, final SelectionCallback<T> callback) {
        return execute(clause, -1, -1, callback);
    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, final ExecutionCallback<T> callback) {
        SQLiteThread<T> thread = new SQLiteThread<T>(this, clause, callback) {

            @Override
            protected T onExecute() {
                return clause.executeLimit1();
            }
        };
        thread.start(transactional);
        return thread;
    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, final int offset, final int limit, final SelectionCallback<T> callback) {
        SQLiteThread<List<T>> thread = new SQLiteThread<List<T>>(this, clause, callback) {

            @Override
            protected List<T> onExecute() {
                return clause.execute(offset, limit);
            }
        };
        thread.start(false);
        return thread;
    }

    public SQLiteThread execute(final SQLiteUpdate.Updater clause, ExecutionCallback<Integer> callback) {
        return execute(clause, -1, callback);
    }

    public SQLiteThread execute(final SQLiteUpdate.Updater clause, final int limit, ExecutionCallback<Integer> callback) {
        SQLiteThread<Integer> thread = new SQLiteThread<Integer>(this, clause, callback) {

            @Override
            protected Integer onExecute() {
                return clause.execute(limit);
            }
        };
        thread.start(transactional);
        return thread;
    }

    //--------------------------------
    public SQLiteThread execute(final SQLiteInsert clause, ExecutionCallback<long[]> callback) {
        SQLiteThread<long[]> thread = new SQLiteThread<long[]>(this, clause, callback) {

            @Override
            protected long[] onExecute() {
                try {
                    return clause.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                    SQLiteException error = new SQLiteException(e.getMessage());
                    error.initCause(e);
                    throw error;
                }
            }
        };
        thread.start(transactional);
        return thread;
    }

    public SQLiteThread execute(final SQLiteMerge clause, ExecutionCallback<List<Object>> callback) {
        SQLiteThread<List<Object>> thread = new SQLiteThread<List<Object>>(this, clause, callback) {

            @Override
            protected List<Object> onExecute() {
                return clause.execute();
            }
        };
        thread.start(transactional);
        return thread;
    }

    public SQLiteThread execute(final SQLitePersist clause, ExecutionCallback<long[]> callback) {
        SQLiteThread<long[]> thread = new SQLiteThread<long[]>(this, clause, callback) {

            @Override
            protected long[] onExecute() {
                return clause.execute();
            }
        };
        thread.start(transactional);
        return thread;
    }

//    public SQLiteThread execute(final SQLitePersist clause, ExecutionCallback<List<Object>> callback) {
//        SQLiteThread<List<Object>> thread = new SQLiteThread<List<Object>>(callback) {
//
//            @Override
//            protected List<Object> onExecute() {
//                return clause.execute();
//            }
//        };
//        thread.start(transactional);
//        return thread;
//    }
    //---------------------------------

//    public SQLiteThread execute(final SQLiteInsert clause, ExecutionCallback<List<Object>> callback) {
//       SQLiteThread<List<Object>> thread = new SQLiteThread<List<Object>>(callback) {
//
//            @Override
//            protected List<Object> onExecute() {
//                try {
//                    clause.execute();
//                    return clause.getInsertions();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                    throw new RuntimeException(e);
//                }
//            }
//        };
//        thread.start(transactional);
//        return thread;
//    }
//
//    public SQLiteThread execute(final SQLiteMerge clause, ExecutionCallback<List<Object>> callback) {
//        SQLiteThread<List<Object>> thread = new SQLiteThread<List<Object>>(callback) {
//
//            @Override
//            protected List<Object> onExecute() {
//                clause.execute();
//                return clause.getMerges();
//            }
//        };
//        thread.start(transactional);
//        return thread;
//    }
//
//    public  SQLiteThread execute(final SQLitePersist clause, ExecutionCallback<List<Object>> callback) {
//        SQLiteThread<List<Object>> thread = new SQLiteThread<List<Object>>(callback) {
//
//            @Override
//            protected List<Object> onExecute() {
//                clause.execute();
//                return clause.getPersists();
//            }
//        };
//        thread.start(transactional);
//        return thread;
//    }
    //---------------------------------

    public SQLiteThread execute(final SQLiteDelete clause, ExecutionCallback<Integer> callback) {
        SQLiteThread<Integer> thread = new SQLiteThread<Integer>(this, clause, callback) {

            @Override
            protected Integer onExecute() {
                return clause.execute();
            }
        };
        thread.start(transactional);
        return thread;
    }

    public interface SelectionCallback<T> extends ExecutionCallback<List<T>> {

    }

    public interface ExecutionCallback<T> {
        void onStart(SQLiteThread thread);

        void onComplete(boolean success);

        void onSuccess(T result);

        void onError(Throwable error);

        void onAborted();
    }
}
