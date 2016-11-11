package istat.android.data.access.sqlite.utils;

public abstract class SQLiteClassLoader {
    abstract String getName();

    abstract String[] getProjections();

    abstract String getPrimaryFieldName();
}
