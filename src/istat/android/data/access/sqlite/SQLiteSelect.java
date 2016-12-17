package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public final class SQLiteSelect extends SQLiteClause<SQLiteSelect> {
    Class<?> clazz;
    String selection;


    SQLiteSelect(SQLite.SQL db, Class<?>... clazz) {
        super(clazz[0], db);
        this.clazz = clazz[0];
        this.selection = this.table;
    }

    public SQLiteSelect join(Class<?> clazz, String on) {
        String join;
        try {
            QueryAble entity = createQueryAble(clazz);
            join = entity.getName();
            selection += " INNER JOIN " + join;
            if (!TextUtils.isEmpty(on)) {
                selection += " ON (" + on + ") ";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteSelect join(String joinTable, String on) {
        String join;
        try {
            join = joinTable;
            selection += " INNER JOIN " + join;
            if (!TextUtils.isEmpty(on)) {
                selection += " ON (" + on + ") ";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private QueryAble createQueryAble(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        //TODO make it better
        return SQLiteModel.fromClass(clazz);
    }

    @Override
    protected Cursor onExecute(SQLiteDatabase db) {
        String[] smartColumns = new String[columns.length];
        String tableName = table;
        for (int i = 0; i < columns.length; i++) {
            smartColumns[i] = tableName + "." + columns[i];
        }
        return db.query(selection, smartColumns, getWhereClause(), getWhereParams(),
                null, null, getOrderBy());

    }


    public int count() {
        Cursor c = onExecute(sql.db);
        int count = c.getCount();
        c.close();
        notifyExecuted();
        return count;

    }

    public <T> T findLast() {
        return findAtIndex(0);
    }

    public <T> T findFirst() {
        return findAtIndex(0);
    }

    public <T> T findAtIndex(int index) {
        return null;
    }

    public <T> List<T> execute(int limit) {

        return null;
    }

    public <T> void execute(List<T> list, int limit) {

    }

    @SuppressWarnings("unchecked")
    public <T> List<T> execute() {
        List<T> list = new ArrayList<T>();
        try {
            Cursor c = onExecute(sql.db);
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    T model = (T) createObjectFromCursor(clazz, c);
                    list.add(model);
                }
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyExecuted();
        return list;

    }

    @SuppressWarnings("unchecked")
    public <T> void execute(List<T> list) {
        if (list == null) {
            list = new ArrayList<T>();
        }
        try {
            Cursor c = onExecute(sql.db);
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    T model = (T) createObjectFromCursor(clazz, c);
                    list.add(model);
                }
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyExecuted();
    }

    /**
     * Create A T instance and fill them from cursor.
     *
     * @param clazz
     * @param c
     * @param <T>
     * @return
     */
    private <T> T createObjectFromCursor(Class<T> clazz, Cursor c) throws InstantiationException, IllegalAccessException {
        //TODO make it better
        SQLiteModel model = SQLiteModel.fromClass(clazz);
        model.fillFromCursor(c);
        T obj = model.asClass(clazz);
        return obj;
    }

    //TODO check if 'selection' or 'table'
    public ClauseBuilder AND_SELECT(SQLiteSelect close) {
        this.whereClause = "(SELECT * FROM " + selection + " WHERE "
                + close.whereClause + ")";
        this.whereParams = close.whereParams;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    public ClauseBuilder OR_SELECT(SQLiteSelect close) {
        this.whereClause = close.whereClause;
        this.whereParams = close.whereParams;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    @Override
    public final String getSQL() {
        String out = "SELECT * FROM " + selection;
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE " + whereClause.trim();
        }
        String[] splits = out.split("\\?");
        String sql = "";
        for (int i = 0; i < (!out.endsWith("?") ? splits.length - 1
                : splits.length); i++) {
            sql += splits[i];
            sql += whereParams.get(i);
        }
        if (!out.endsWith("?")) {
            sql += splits[splits.length - 1];
        }
        return sql;
    }

}
