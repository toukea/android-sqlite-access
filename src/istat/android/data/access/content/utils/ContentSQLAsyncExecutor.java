package istat.android.data.access.content.utils;

import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;


import java.util.List;

import istat.android.data.access.content.ContentSQLPersist;
import istat.android.data.access.content.ContentSQLSelect;
import istat.android.data.access.content.ContentSQLDelete;
import istat.android.data.access.content.ContentSQLInsert;
import istat.android.data.access.content.ContentSQLMerge;
import istat.android.data.access.content.ContentSQLUpdate;

/**
 * Created by istat on 08/02/17.
 */

public final class ContentSQLAsyncExecutor {
    private Handler handler = new Handler(Looper.getMainLooper());
    boolean transactional = true;

    public ContentSQLAsyncExecutor(Handler handler) {
        this(handler, true);
    }

    public ContentSQLAsyncExecutor(Handler handler, boolean transactional) {
        this.handler = handler;
        this.transactional = transactional;
    }

    public Handler getHandler() {
        return handler;
    }

    public ContentSQLAsyncExecutor() {
        this(true);
    }

    public ContentSQLAsyncExecutor(boolean transactional) {
        this.transactional = transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public <T> ContentSQLThread execute(final ContentSQLSelect clause, int limit, final SelectionCallback<T> callback) {
        return execute(clause, -1, limit, callback);
    }

    public <T> ContentSQLThread execute(final ContentSQLSelect clause, final SelectionCallback<T> callback) {
        return execute(clause, -1, -1, callback);
    }

    public <T> ContentSQLThread execute(final ContentSQLSelect clause, final ExecutionCallback<T> callback) {
        ContentSQLThread<T> thread = new ContentSQLThread<T>(this, clause, callback) {

            @Override
            protected T onExecute() {
                return clause.executeLimit1();
            }
        };
        thread.start(transactional);
        return thread;
    }

    public <T> ContentSQLThread execute(final ContentSQLSelect clause, final int offset, final int limit, final SelectionCallback<T> callback) {
        ContentSQLThread<List<T>> thread = new ContentSQLThread<List<T>>(this, clause, callback) {

            @Override
            protected List<T> onExecute() {
                return clause.execute(offset, limit);
            }
        };
        thread.start(false);
        return thread;
    }

    public ContentSQLThread execute(final ContentSQLUpdate.Updater clause, ExecutionCallback<Integer> callback) {
        return execute(clause, -1, callback);
    }

    public ContentSQLThread execute(final ContentSQLUpdate.Updater clause, final int limit, ExecutionCallback<Integer> callback) {
        ContentSQLThread<Integer> thread = new ContentSQLThread<Integer>(this, clause, callback) {

            @Override
            protected Integer onExecute() {
                return clause.execute(limit);
            }
        };
        thread.start(transactional);
        return thread;
    }

    //--------------------------------
    public ContentSQLThread execute(final ContentSQLInsert clause, ExecutionCallback<long[]> callback) {
        ContentSQLThread<long[]> thread = new ContentSQLThread<long[]>(this, clause, callback) {

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

    public ContentSQLThread execute(final ContentSQLMerge clause, ExecutionCallback<List<Object>> callback) {
        ContentSQLThread<List<Object>> thread = new ContentSQLThread<List<Object>>(this, clause, callback) {

            @Override
            protected List<Object> onExecute() {
                return clause.execute();
            }
        };
        thread.start(transactional);
        return thread;
    }

    public ContentSQLThread execute(final ContentSQLPersist clause, ExecutionCallback<long[]> callback) {
        ContentSQLThread<long[]> thread = new ContentSQLThread<long[]>(this, clause, callback) {

            @Override
            protected long[] onExecute() {
                return clause.execute();
            }
        };
        thread.start(transactional);
        return thread;
    }

//    public ContentSQLThread execute(final ContentSQLPersist clause, ExecutionCallback<List<Object>> callback) {
//        ContentSQLThread<List<Object>> thread = new ContentSQLThread<List<Object>>(callback) {
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

//    public ContentSQLThread execute(final ContentSQLInsert clause, ExecutionCallback<List<Object>> callback) {
//       ContentSQLThread<List<Object>> thread = new ContentSQLThread<List<Object>>(callback) {
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
//    public ContentSQLThread execute(final ContentSQLMerge clause, ExecutionCallback<List<Object>> callback) {
//        ContentSQLThread<List<Object>> thread = new ContentSQLThread<List<Object>>(callback) {
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
//    public  ContentSQLThread execute(final ContentSQLPersist clause, ExecutionCallback<List<Object>> callback) {
//        ContentSQLThread<List<Object>> thread = new ContentSQLThread<List<Object>>(callback) {
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

    public ContentSQLThread execute(final ContentSQLDelete clause, ExecutionCallback<Integer> callback) {
        ContentSQLThread<Integer> thread = new ContentSQLThread<Integer>(this, clause, callback) {

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
        void onStart(ContentSQLThread thread);

        void onComplete(boolean success);

        void onSuccess(T result);

        void onError(Throwable error);

        void onAborted();
    }
}
