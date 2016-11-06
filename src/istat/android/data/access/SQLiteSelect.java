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

    SQLiteSelect(SQLiteDatabase db, Class<?>... clazz) {
        super(clazz[0], db);
        this.clazz = clazz[0];
    }

    public SQLiteSelect join(Class<?> clazz, String on) {

        try {
            QueryAble entity = createQueryAble(clazz);
            join = entity.getEntityName();
            table += " INNER JOIN " + join;
            if (!TextUtils.isEmpty(on)) {
                table += " ON (" + on + ") ";
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
        return db.query(table, projection, getWhereClause(), getWhereParams(),
                null, null, getOrderBy());

    }


    public int count(SQLiteDatabase db) {
        Cursor c = onExecute(db);
        int count = c.getCount();
        c.close();
        return count;

    }

    @SuppressWarnings("unchecked")
    public <T> List<T> execute() {
        List<T> list = new ArrayList<T>();
        try {
            Cursor c = onExecute(db);
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    T model = (T) createModelFromCursor(clazz, c);
                    list.add(model);
                }
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;

    }

    @SuppressWarnings("unchecked")
    public <T> void execute(List<T> list) {
        if (list == null) {
            list = new ArrayList<T>();
        }
        try {
            Cursor c = onExecute(db);
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    T model = (T) createModelFromCursor(clazz, c);
                    list.add(model);
                }
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create A T instance and fill them from cursor.
     *
     * @param clazz
     * @param c
     * @param <T>
     * @return
     */
    private <T> T createModelFromCursor(Class<T> clazz, Cursor c) throws InstantiationException, IllegalAccessException {
        //TODO make it better
        SQLiteModel model = SQLiteModel.fromClass(clazz);
        model.fillFromCursor(c);
        T obj = model.asClass(clazz);
        return obj;
    }

    public ClauseBuilder AND_SELECT(SQLiteSelect close) {
        this.whereClause = "(SELECT * FROM " + table + " WHERE "
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
        return super.getSQL();
    }

}
