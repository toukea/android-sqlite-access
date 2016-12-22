package istat.android.data.access.sqlite;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class SQLiteSelect extends SQLiteClause<SQLiteSelect> {
    Class<?> clazz;
    String selection;


    SQLiteSelect(SQLite.SQL db, Class<?>... clazz) {
        super(clazz[0], db);
        this.clazz = clazz[0];
        this.selection = this.table;
    }

    public SQLiteJoinSelect joinOn(Class<?> clazz, String on) {
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
        return new SQLiteJoinSelect(this.sql, this.clazz);
    }

    public SQLiteJoinSelect joinOn(String joinTable, String on) {
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
        return new SQLiteJoinSelect(this.sql, this.clazz);
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
    public final String getStatement() {
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

    public class ClauseJoinBuilder {
        SQLiteJoinSelect joinSelect;
        Class<?> clazz;

        ClauseJoinBuilder(Class<?> clazz, SQLiteJoinSelect selection) {
            this.clazz = clazz;
            this.joinSelect = selection;
        }

        public ClauseSubJoinBuilder on(Class<?> clazz, String name) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                name = buildWhereParam(model.getName(), name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseSubJoinBuilder(joinSelect, name);
        }


        public ClauseBuilder where(Class<?> clazz, String column) {
            try {
                Class<?> selectionClass = joinSelect.clazz;
                Class<?> joinClass = this.clazz;
                SQLiteModel selectionModel = SQLiteModel.fromClass(selectionClass);
                SQLiteModel joinModel = SQLiteModel.fromClass(selectionClass);
                Field[] fields = selectionModel.getNestedTableFields();
                String nestedPrimaryKey = joinModel.getPrimaryFieldName();
                String foreignKey = "";
                for (Field field : fields) {
                    if (field.getType().isAssignableFrom(joinClass)) {
                        foreignKey = selectionModel.getFieldNestedMappingName(field);
                    }
                }
                return on(selectionClass, foreignKey).equalTo(joinClass, nestedPrimaryKey).where(clazz, column);
            } catch (Exception e) {
                return defaultWhere(clazz, column);
            }
        }

        private ClauseBuilder defaultWhere(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = buildWhereParam(model.getName(), column);
                else
                    whereClause += " AND " + buildWhereParam(model.getName(), column);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseBuilder(TYPE_CLAUSE_AND);
        }

        public ClauseJoinBuilder innerJoin(Class<?> clazz) {
            return join(clazz, TYPE_JOIN_INNER);
        }

        public ClauseJoinBuilder leftJoin(Class<?> clazz) {
            return join(clazz, TYPE_JOIN_LEFT);
        }

        public ClauseJoinBuilder rightJoin(Class<?> clazz) {
            return join(clazz, TYPE_JOIN_RIGHT);
        }

        public ClauseJoinBuilder fullJoin(Class<?> clazz) {
            return join(clazz, TYPE_JOIN_FULL);
        }

    }

    public class ClauseSubJoinBuilder {
        SQLiteJoinSelect joinSelect;
        String columnJoinName;

        ClauseSubJoinBuilder(SQLiteJoinSelect joinSelect, String name) {
            this.joinSelect = joinSelect;
            this.columnJoinName = name;

        }

        public SQLiteJoinSelect equalTo(Class<?> clazz, String name) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                name = buildWhereParam(model.getName(), name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            selection += " ON (" + columnJoinName + "=" + name + ") ";
            this.joinSelect.selection = selection;
            return joinSelect;
        }
    }

    public final class SQLiteJoinSelect extends SQLiteSelect {

        SQLiteJoinSelect(SQLite.SQL db, Class<?>... clazz) {
            super(db, clazz);
            this.whereClause = SQLiteSelect.this.whereClause;
            this.whereParams = SQLiteSelect.this.whereParams;
            this.selection = SQLiteSelect.this.selection;
            this.table = SQLiteSelect.this.table;
        }

        public ClauseBuilder where(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = buildWhereParam(model.getName(), column);
                else
                    whereClause += " AND " + buildWhereParam(model.getName(), column);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseBuilder(TYPE_CLAUSE_AND);
        }

        public ClauseBuilder or(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = buildWhereParam(model.getName(), column);
                else
                    whereClause += " OR " + buildWhereParam(model.getName(), column);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseBuilder(TYPE_CLAUSE_OR);
        }

        public ClauseBuilder and(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = buildWhereParam(model.getName(), column);
                else
                    whereClause += " AND " + buildWhereParam(model.getName(), column);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseBuilder(TYPE_CLAUSE_AND);
        }

        @Override
        protected String buildWhereParam(String column) {
            return column;
        }
    }

    public final static String TYPE_JOIN_INNER = " INNER ", TYPE_JOIN_LEFT = " LEFT ", TYPE_JOIN_RIGHT = " RIGHT ", TYPE_JOIN_FULL = " FULL ";

    public ClauseJoinBuilder join(Class<?> clazz) {
        return join(clazz, TYPE_JOIN_INNER);
    }

    public ClauseJoinBuilder innerJoin(Class<?> clazz) {
        return join(clazz, TYPE_JOIN_INNER);
    }

    public ClauseJoinBuilder leftJoin(Class<?> clazz) {
        return join(clazz, TYPE_JOIN_LEFT);
    }

    public ClauseJoinBuilder rightJoin(Class<?> clazz) {
        return join(clazz, TYPE_JOIN_RIGHT);
    }

    public ClauseJoinBuilder fullJoin(Class<?> clazz) {
        return join(clazz, TYPE_JOIN_FULL);
    }

    public ClauseJoinBuilder join(Class<?> clazz, String joinType) {
        String join;
        try {
            QueryAble entity = createQueryAble(clazz);
            join = entity.getName();
            selection += joinType + "JOIN " + join;
//            if (!TextUtils.isEmpty(on)) {
//                selection += " ON (" + on + ") ";
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SQLiteJoinSelect joinSelection = new SQLiteJoinSelect(this.sql, this.clazz);
        return new ClauseJoinBuilder(clazz, joinSelection);
    }

}
