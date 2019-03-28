package istat.android.data.access.content.interfaces;

import android.database.Cursor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import istat.android.data.access.content.ContentSQLModel;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;
import istat.android.data.access.content.utils.ContentSQLThread;

/**
 * Created by istat on 31/07/17.
 */

public interface SelectionExecutable {
    SelectionExecutable limit(int offset, int limit);

    List<ContentSQLModel> getResults() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    ContentSQLModel getSingleResult() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;


    Cursor getCursor();

    <T> void execute(List<T> list);

    <T> List<T> execute();

    <T> List<T> execute(Class<T> clazz);

    <T> void execute(List<T> list, Class<?> clazz);

    <T> T executeLimit1();

    <T> ContentSQLThread<List<T>> executeAsync();

    <T> ContentSQLThread<T> executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<T> callback);

    <T> ContentSQLThread<List<T>> executeAsync(final ContentSQLAsyncExecutor.SelectionCallback<T> callback);

    String getStatement();

    int count();

    <T> List<T> fetch();

    <T> List<T> fetch(Class<T> clazz);

    <T> void fetch(List<T> list, Class<?> clazz);
}
