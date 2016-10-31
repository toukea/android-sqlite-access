package istat.android.data.access;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class SQLiteSelect extends SQLiteClause<SQLiteSelect> {
    Class<? extends QueryAble> clazz;
    String join;

    SQLiteSelect(Class<? extends QueryAble> clazz, SQLiteDatabase db) {
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
    public <T extends QueryAble> List<T> execute(SQLiteDatabase db) {
        List<T> list = new ArrayList<T>();
        Cursor c = onExecute(db);
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                list.add((T) createFromCursor(clazz, c));
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
    public <T extends QueryAble> void execute(SQLiteDatabase db, List<T> list) {
        if (list == null)
            list = new ArrayList<T>();
        Cursor c = onExecute(db);
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                list.add((T) createFromCursor(clazz, c));
            }
        }
        c.close();

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
