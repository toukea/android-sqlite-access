package istat.android.data.access.sqlite.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 24/03/17.
 */

public abstract class SQLiteThread<T> extends Thread {
    boolean running = false;
    SQLiteAsyncExecutor.ExecutionCallback<T> callback;

    SQLiteThread(SQLiteAsyncExecutor.ExecutionCallback<T> callback) {
        this.callback = callback;
    }

    @Override
    public final void run() {
        try {
            notifySuccess(onExecute());
        } catch (Exception e) {
            notifyError(e);
        }
    }

    protected abstract T onExecute();

    @Override
    public synchronized void start() {
        running = true;
        super.start();
        notifyStarted(this);
    }

    @Override
    public void interrupt() {
        running = false;
        if (callback != null) {
            callback.onAborted();
        }
        super.interrupt();
    }

    public void cancel() {
        interrupt();
    }

    T result;

    protected void notifySuccess(T result) {
        this.result = result;
        if (callback != null) {
            notifyCompleted(true);
            callback.onSuccess(result);
        }

    }

    protected void notifyError(Throwable e) {
        if (callback != null) {
            notifyCompleted(false);
            callback.onError(e);
        }
        int when = WHEN_FAILED;
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(when);
        executeWhen(runnableList, when);

    }

    private void notifyCompleted(boolean state) {
        int when = WHEN_ANYWAY;
        callback.onComplete(state);
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(when);
        executeWhen(runnableList, WHEN_ANYWAY);
    }

    private void notifyStarted(SQLiteThread thread) {
        int when = WHEN_BEGIN;
        if (callback != null) {
            callback.onStart(thread);
        }
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(when);
        executeWhen(runnableList, when);
    }

    public final static int WHEN_BEGIN = -1;
    public final static int WHEN_ANYWAY = 0;
    public final static int WHEN_SUCCEED = 1;
    public final static int WHEN_ABORTION = 3;
    public final static int WHEN_FAILED = 4;

    public interface WhenCallback<T> {
        void onWhen(T result, int when);
    }

    public interface PromiseCallback<T> {
        void onPromise(T promise);
    }

    final ConcurrentHashMap<Runnable, Integer> executedRunnable = new ConcurrentHashMap<Runnable, Integer>();
    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> runnableTask = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>>();

    public SQLiteThread runWhen(final WhenCallback<T> callback, final int... when) {
        if (callback == null)
            return this;
        return runWhen(new Runnable() {
            @Override
            public void run() {
                T resp = getResult();
                int when = executedRunnable.get(this);
                callback.onWhen(resp, when);
            }
        }, when);
    }

    public SQLiteThread runWhen(Runnable runnable, int... when) {
        if (runnable == null) {
            return this;
        }
        for (int value : when) {
            addWhen(runnable, value);
        }
        return this;
    }

    private void addWhen(Runnable runnable, int conditionTime) {
        if (!isWhenContain(runnable, conditionTime)) {
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(conditionTime);
            if (runnableList == null) {
                runnableList = new ConcurrentLinkedQueue<Runnable>();
            }
            runnableList.add(runnable);
            runnableTask.put(conditionTime, runnableList);
        }
    }

    private boolean isWhenContain(Runnable run, int conditionTime) {
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(conditionTime);
        if (runnableList == null || runnableList.isEmpty()) {
            return false;
        }
        return runnableList.contains(run);
    }

    private void executeWhen(ConcurrentLinkedQueue<Runnable> runnableList, int when) {
        if (runnableList != null && runnableList.size() > 0) {
            for (Runnable runnable : runnableList) {
                if (!executedRunnable.contains(runnable)) {
                    runnable.run();
                    executedRunnable.put(runnable, when);
                }
            }
        }
    }

    public final static class SQLitePromise<T> {

        SQLiteThread query;

        public SQLiteThread getQuery() {
            return query;
        }

        SQLitePromise(SQLiteThread query) {
            this.query = query;
        }

        public SQLitePromise then(final PromiseCallback<T> callback) {
            if (callback == null) {
                return this;
            }
            query.runWhen(new WhenCallback<T>() {
                @Override
                public void onWhen(T result, int when) {
                    callback.onPromise(result);
                }
            }, WHEN_SUCCEED);
            return this;
        }

        public SQLitePromise then(Runnable runnable) {
            if (runnable == null) {
                return this;
            }
            query.runWhen(runnable, WHEN_SUCCEED);
            return this;
        }

        public SQLitePromise error(final PromiseCallback<Throwable> pCallback, int when) {
            if (pCallback == null) {
                return this;
            }
//            WhenCallback<T> callback = new WhenCallback<T>() {
//                @Override
//                public void onWhen(T result, int when) {
//                    pCallback.onPromise(result);
//                }
//            };
//            if (when != WHEN_FAILED && when != WHEN_ABORTION) {
//                query.runWhen(callback, WHEN_FAILED, WHEN_ABORTION);
//            } else {
//                query.runWhen(callback, when);
//            }
            return this;
        }

        public void error(WhenCallback<Throwable> callback) {
            if (callback == null) {
                return;
            }
            query.runWhen(callback, WHEN_FAILED, WHEN_ABORTION);
        }

        public SQLitePromise error(Runnable runnable) {
            if (runnable == null) {
                return this;
            }
            query.runWhen(runnable, WHEN_FAILED, WHEN_ABORTION);
            return this;
        }
    }

    public boolean dismissAllRunWhen() {
        boolean isEmpty = runnableTask.isEmpty();
        runnableTask.clear();
        return !isEmpty;
    }

    public boolean dismissRunWhen(int... when) {
        boolean isEmpty = false;
        for (int i : when) {
            ConcurrentLinkedQueue<Runnable> runnables = runnableTask.get(i);
            if (runnables != null) {
                isEmpty &= runnables.isEmpty();
                runnables.clear();
            }
        }
        return !isEmpty;
    }

    public boolean dismissCallback() {
        boolean dismiss = callback != null;
        callback = null;
        return dismiss;
    }

    public SQLitePromise<T> then(Runnable runnable) {
        SQLitePromise<T> promise = new SQLitePromise<T>(this);
        promise.then(runnable);
        return promise;
    }

    public SQLitePromise<T> then(PromiseCallback<T> callback) {
        SQLitePromise<T> promise = new SQLitePromise<T>(this);
        promise.then(callback);
        return promise;
    }

    public SQLitePromise<T> error(Runnable runnable) {
        SQLitePromise<T> promise = new SQLitePromise<T>(this);
        promise.error(runnable);
        return promise;
    }

    public SQLitePromise<T> error(WhenCallback<Throwable> callback) {
        SQLitePromise<T> promise = new SQLitePromise<T>(this);
        promise.error(callback);
        return promise;
    }

    public SQLitePromise<T> error(PromiseCallback<Throwable> callback, int when) {
        SQLitePromise<T> promise = new SQLitePromise<T>(this);
        promise.error(callback, when);
        return promise;
    }

    public T getResult() {
        return result;
    }
}