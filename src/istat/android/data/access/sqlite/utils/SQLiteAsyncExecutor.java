package istat.android.data.access.sqlite.utils;

import android.os.Handler;
import android.os.Looper;


import istat.android.data.access.sqlite.SQLiteDelete;
import istat.android.data.access.sqlite.SQLiteInsert;
import istat.android.data.access.sqlite.SQLiteSelect;
import istat.android.data.access.sqlite.SQLiteUpdate;

/**
 * Created by istat on 08/02/17.
 */

public class SQLiteAsyncExecutor {
    private Handler handler = new Handler(Looper.getMainLooper());

    public Thread execute(final SQLiteSelect clause, final ExecutionCallback callback) {
        Runnable runnable = null;
        return execute(runnable);
    }

    public Thread execute(SQLiteUpdate clause, ExecutionCallback callback) {
        Runnable runnable = null;
        return execute(runnable);
    }

    public Thread execute(SQLiteInsert clause, ExecutionCallback callback) {
        Runnable runnable = null;
        return execute(runnable);
    }

    public Thread execute(SQLiteDelete clause, ExecutionCallback callback) {
        Runnable runnable = null;
        return execute(runnable);
    }

    private Thread execute(final Runnable runnable) {
        Thread thread = new Thread() {
            boolean running = false;

            @Override
            public void run() {
                if (runnable != null) {
                    runnable.run();
                }
            }

            @Override
            public synchronized void start() {
                super.start();
                running = true;
            }

            @Override
            public void interrupt() {
                super.interrupt();
                running = false;
            }
        };
        return thread;
    }

    public interface ExecutionCallback<T> {
        void onCompleted(boolean success);

        void onSuccess(T result);

        void onError(Throwable error);
    }
}
