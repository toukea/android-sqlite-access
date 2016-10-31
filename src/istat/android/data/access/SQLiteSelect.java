package istat.android.data.access;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class SQLiteSelect extends SQLiteClause<SQLiteSelect> {
    Class<?> clazz;
    String join;

    SQLiteSelect(Class<?> clazz, SQLiteDatabase db) {
        super(clazz, db);
        this.clazz = clazz;
    }

    SQLiteSelect(SQLiteDatabase db, Class<? extends QueryAble>... clazz) {
        super(clazz[0], db);
        this.clazz = clazz[0];
    }

    public SQLiteSelect join(Class<? extends QueryAble> clazz, String on) {
        QueryAble entity = createEntityInstance(clazz);
        join = entity.getEntityName();
        table += " INNER JOIN " + join;
        if (!TextUtils.isEmpty(on)) {
            table += " ON (" + on + ") ";
        }
        return this;
    }

    @Override
    protected Cursor onExecute(SQLiteDatabase db) {
        return db.query(table, projection, getWhereClose(), getWhereParams(),
                null, null, getOrderBy());

    }

    @SuppressWarnings("unchecked")
    public <T extends QueryAble> List<T> execute() {
        List<T> list = new ArrayList<T>();
        Cursor c = onExecute(db);
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                T model = createModelFromCursor(clazz, c);
                list.add(model);
            }
        }
        c.close();
        return list;

    }

    public int count(SQLiteDatabase db) {
        Cursor c = onExecute(db);
        int count = c.getCount();
        c.close();
        return count;

    }

    @SuppressWarnings("unchecked")
    public <T> void execute(List<T> list) {
        if (list == null)
            list = new ArrayList<T>();
        Cursor c = onExecute(db);
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                T model = createModelFromCursor(clazz, c);
                list.add(model);
            }
        }
        c.close();

    }

    /**
     * Create A T instance and fill them from cursor.
     *
     * @param clazz
     * @param c
     * @param <T>
     * @return
     */
    private <T> T createModelFromCursor(Class<?> clazz, Cursor c) {
        //TODO populate this fuction.
        return null;
    }

    public ClauseBuilder AND_SELECT(SQLiteSelect close) {
        this.whereClose = "(SELECT * FROM " + table + " WHERE "
                + close.whereClose + ")";
        this.whereParams = close.whereParams;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    public ClauseBuilder OR_SELECT(SQLiteSelect close) {
        this.whereClose = close.whereClose;
        this.whereParams = close.whereParams;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    @Override
    public final String getSQL() {
        return super.getSQL();
    }

}
