package istat.android.data.access.sqlite.interfaces;

import android.database.Cursor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import istat.android.data.access.sqlite.SQLiteModel;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

/**
 * Created by istat on 31/07/17.
 */

public interface SelectionExecutable {
    SelectionExecutable limit(int offset, int limit);

    List<SQLiteModel> getResults() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    SQLiteModel getSingleResult() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;


    Cursor getCursor();

    <T> void execute(List<T> list);

    <T> List<T> execute();

    <T> List<T> execute(Class<T> clazz);

    <T> void execute(List<T> list, Class<?> clazz);

    <T> T executeLimit1();

    <T> SQLiteThread<List<T>> executeAsync();

    <T> SQLiteThread<T> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<T> callback);

    <T> SQLiteThread<List<T>> executeAsync(final SQLiteAsyncExecutor.SelectionCallback<T> callback);

    String getStatement();

    int count();

    <T> List<T> fetch();

    <T> List<T> fetch(Class<T> clazz);

    <T> void fetch(List<T> list, Class<?> clazz);
}
