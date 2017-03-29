package istat.android.data.access.sqlite;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import istat.android.data.access.sqlite.interfaces.QueryAble;
import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public class SQLiteSelect extends SQLiteClause<SQLiteSelect> {
    public final static String ORDER_BY_DESC = "DESC", ORDER_BY_ASC = "ASC";
    public final static int TYPE = 0;
    Class<?> clazz;
    String selectionTable;
    boolean distinct = false;


    SQLiteSelect(SQLite.SQL db, Class<?>... clazz) {
        super(clazz[0], db);
        this.clazz = clazz[0];
        this.selectionTable = this.table;
    }

    public SQLiteJoinSelect joinOn(Class<?> clazz, String on) {
        String join;
        try {
            QueryAble entity = createQueryAble(clazz);
            join = entity.getName();
            selectionTable += " INNER JOIN " + join;
            if (!TextUtils.isEmpty(on)) {
                selectionTable += " ON (" + on + ") ";
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
            selectionTable += " INNER JOIN " + join;
            if (!TextUtils.isEmpty(on)) {
                selectionTable += " ON (" + on + ") ";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SQLiteJoinSelect(this.sql, this.clazz);
    }

//    protected SQLiteSelect distinct() {
//        return distinct(true);
//    }

    protected SQLiteSelect distinct(boolean enable) {
        distinct = enable;
        return this;
    }

    private QueryAble createQueryAble(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        //TODO make it better
        return SQLiteModel.fromClass(clazz);
    }

    @Override
    protected Cursor onExecute(SQLiteDatabase db) {
        notifyExecuting();
        String[] smartColumns = new String[columns.length];
        String tableName = table;
        for (int i = 0; i < columns.length; i++) {
            smartColumns[i] = tableName + "." + columns[i];
        }
        return db.query(distinct, selectionTable, smartColumns, getWhereClause(), getWhereParams(),
                getGroupBy(), getHaving(), getOrderBy(), getLimit());
    }


    public int count() {
        Cursor c = onExecute(sql.db);
        int count = c.getCount();
        c.close();
        notifyExecuted();
        return count;
    }

    public <T> List<T> execute(int offset, int limit) {
        String limitS;
        if (limit < 0) {
            limitS = null;
        } else {
            if (offset < 0) {
                offset = 0;
            }
            limitS = offset + ", " + limit;
        }
        return execute(limitS);
    }

    public <T> void execute(List<T> list, int offset, int limit) {
        String limitS;
        if (limit < 0) {
            limitS = null;
        } else {
            if (offset < 0) {
                offset = 0;
            }
            limitS = offset + ", " + limit;
        }
        execute(list, limitS);
    }

    public <T> List<T> execute(int limit) {
        return execute(limit > 0 ? "" + limit : null);
    }

    public <T> void execute(List<T> list, int limit) {
        execute(list, limit > 0 ? "" + limit : null);
    }

    public <T> List<T> execute(String limit) {
        this.limit = limit;
        return execute();
    }

    public <T> void execute(List<T> list, String limit) {
        this.limit = limit;
        execute(list);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> execute() {
        List<T> list = new ArrayList<T>();
        execute(list);
        return list;
    }

    public <T> T executeLimitOne() {
        List<T> results = execute(1);
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(List<T> list) {
        if (list == null) {
            return;
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
            notifyExecutionSucceed(TYPE, this, list);
        } catch (Exception e) {
            e.printStackTrace();
            notifyExecutionFail(e);
        }
        notifyExecuted();
    }

    public <T> List<T> execute(Class<T> clazz) {
        List<T> list = new ArrayList<T>();
        try {
            Cursor c = onExecute(sql.db);
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    T model = createObjectFromCursor(clazz, c);
                    list.add(model);
                }
            }
            c.close();
            notifyExecutionSucceed(TYPE, this, list);
        } catch (Exception e) {
            e.printStackTrace();
            notifyExecutionFail(e);
        }
        notifyExecuted();
        return list;
    }

    public <T> SQLiteThread<List<T>> executeAsync() {
        return executeAsync(-1, -1, null);
    }

    public <T> SQLiteThread<List<T>> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<List<T>> callback) {
        return executeAsync(-1, -1, callback);
    }

    public <T> SQLiteThread<List<T>> executeAsync(final int limit, final SQLiteAsyncExecutor.ExecutionCallback<List<T>> callback) {
        return executeAsync(-1, limit, callback);
    }

    public <T> SQLiteThread<List<T>> executeAsync(final int offset, final int limit, final SQLiteAsyncExecutor.ExecutionCallback<List<T>> callback) {
        SQLiteAsyncExecutor asyncExecutor = new SQLiteAsyncExecutor();
        return asyncExecutor.execute(this, offset, limit, callback);
    }

    /**
     * Create A T instance and fill them from cursor.
     *
     * @param clazz
     * @param c
     * @param <T>
     * @return
     */
    private <T> T createObjectFromCursor(Class<T> clazz, Cursor c) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        //TODO make it better
        SQLiteModel model = SQLiteModel.fromClass(clazz);
        model.fillFromCursor(c);
        T obj = model.asClass(clazz);
        return obj;
    }

//    //TODO check if 'selectionTable' or 'table'
//    public ClauseBuilder AND_SELECT(SQLiteSelect clause) {
//        this.whereClause = "(SELECT * FROM " + selectionTable + " WHERE "
//                + clause.whereClause + ")";
//        this.whereParams = clause.whereParams;
//        return new ClauseBuilder(TYPE_CLAUSE_AND);
//    }
//
//    public ClauseBuilder OR_SELECT(SQLiteSelect clause) {
//        this.whereClause = clause.whereClause;
//        this.whereParams = clause.whereParams;
//        return new ClauseBuilder(TYPE_CLAUSE_OR);
//    }

    final String getSql() {
        String columns = "";//"*";
        for (int i = 0; i < this.columns.length; i++) {
            columns += this.columns[i];
            if (i < this.columns.length - 1) {
                columns += ",";
            }
        }
        String out = "SELECT " + columns + " FROM " + selectionTable;
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE " + whereClause.toString().trim();
        }
        if (!TextUtils.isEmpty(orderBy)) {
            out += " ORDER BY " + orderBy;
        }
        if (!TextUtils.isEmpty(groupBy)) {
            out += " GROUP BY " + groupBy;
        }
        if (!TextUtils.isEmpty(having)) {
            out += " HAVING " + having;
        }
        if (!TextUtils.isEmpty(limit)) {
            out += " LIMIT " + limit;
        }
        return out;
    }

    @Override
    public String toString() {
        return getStatement();
    }

    public String toString(boolean details) {
        return details ? getSql() : getStatement();
    }

    @Override
    public final String getStatement() {
        String columnParam = "*";
        try {
            SQLiteModel entity = SQLiteModel.fromClass(clazz);
            if (entity.getColumns().length != this.columns.length) {
                columnParam = "";
                for (int i = 0; i < this.columns.length; i++) {
                    columnParam += this.columns[i];
                    if (i < this.columns.length - 1) {
                        columnParam += ",";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String out = "SELECT " + columnParam + " FROM " + selectionTable;
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE " + whereClause.toString().trim();
        }
        String sql = compute(out, this.whereParams);
        if (!TextUtils.isEmpty(orderBy)) {
            sql += " ORDER BY " + orderBy;
        }
        if (!TextUtils.isEmpty(groupBy)) {
            sql += " GROUP BY " + groupBy;
        }
        if (!TextUtils.isEmpty(having)) {
            this.having = new StringBuilder(compute(this.having.toString(), havingWhereParams));
            sql += " HAVING " + having;
        }
        if (!TextUtils.isEmpty(limit)) {
            sql += " LIMIT " + limit;
        }
        return sql;
    }

    public class ClauseJoinSelectBuilder {
        int type = 0;
        SQLiteJoinSelect selectClause;

        public ClauseJoinSelectBuilder(int type, SQLiteJoinSelect selectClause) {
            this.type = type;
            this.selectClause = selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect equalTo(Object value) {
            prepare(value);
            whereClause.append(" = ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect notEqualTo(Object value) {
            prepare(value);
            whereClause.append(" = ? ");
            return selectClause;
        }

        public <T> SQLiteJoinSelect notIn(T... value) {
            return in(false, value);
        }

        public <T> SQLiteJoinSelect in(T... value) {
            return in(true, value);
        }

        private <T> SQLiteJoinSelect in(boolean truth, T[] value) {
            String valueIn = "";
            for (int i = 0; i < value.length; i++) {
                if (value[i] instanceof Number) {
                    valueIn += value[i];
                } else {
                    valueIn += "'" + value[i] + "'";
                }
                if (i < value.length - 1) {
                    valueIn += ", ";
                }
            }
            if (!valueIn.startsWith("(") && !valueIn.endsWith(")")) {
                valueIn = "(" + valueIn + ")";
            }
            whereClause.append((truth ? "" : " NOT ") + " IN " + valueIn);
            return selectClause;
        }

        @Deprecated
        public SQLiteJoinSelect equal(Object value) {
            return equalTo(value);
        }

        public SQLiteJoinSelect greatThan(Object value) {
            return greatThan(value, false);
        }

        public SQLiteJoinSelect lessThan(Object value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect greatThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect like(Object value) {
            prepare(value);
            whereClause.append(" like ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect notLike(Object value) {
            prepare(value);
            whereClause.append(" NOT like ? ");
            return selectClause;
        }

        //------------------------------------------------
        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect equalTo(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" = (" + value + ") ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect notEqualTo(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" = (" + value + ") ");
            return selectClause;
        }

        public SQLiteJoinSelect in(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" IN (" + value.getSql() + ") ");
            return selectClause;
        }

        public SQLiteJoinSelect notIn(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" NOT IN (" + value.getSql() + ") ");
            return selectClause;
        }

        public SQLiteJoinSelect greatThan(SQLiteSelect value) {
            return greatThan(value, false);
        }

        public SQLiteJoinSelect lessThan(SQLiteSelect value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect greatThan(SQLiteSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect lessThan(SQLiteSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect like(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" like (" + value.getSql() + ")");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public SQLiteJoinSelect notLike(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" NOT like (" + value.getSql() + ")");
            return selectClause;
        }
        //------------------------------------------------


        private void prepare(Object value) {
            whereParams.add(value + "");
            switch (type) {
                case TYPE_CLAUSE_AND:

                    break;
                case TYPE_CLAUSE_OR:

                    break;
                default:
                    break;

            }
        }

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

        private SQLiteJoinSelect buildSubJoin() throws IllegalAccessException, InstantiationException {
            Class<?> selectionClass = joinSelect.clazz;
            Class<?> joinClass = this.clazz;
            SQLiteModel selectionModel = SQLiteModel.fromClass(selectionClass);
            SQLiteModel joinModel = SQLiteModel.fromClass(selectionClass);
            Field[] fields = selectionModel.getNestedTableFields();
            String nestedPrimaryKey = joinModel.getPrimaryFieldName();
            String foreignKey = selectionModel.getPrimaryFieldName();
            for (Field field : fields) {
                if (field.getType().isAssignableFrom(joinClass)) {
                    foreignKey = selectionModel.getFieldNestedMappingName(field);
                }
            }
            return on(selectionClass, foreignKey).equalTo(joinClass, nestedPrimaryKey);
        }

        public ClauseJoinSelectBuilder where(Class<?> clazz, String column) {
            try {
                return buildSubJoin().where(clazz, column);
            } catch (Exception e) {
                return defaultWhere(clazz, column);
            }
        }

        private ClauseJoinSelectBuilder defaultWhere(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildWhereParam(model.getName(), column));
                else
                    whereClause.append(" AND " + buildWhereParam(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, joinSelect);
        }

        public ClauseJoinBuilder innerJoin(Class<?> clazz) {
            try {
                return buildSubJoin().innerJoin(clazz);//join(clazz, TYPE_JOIN_INNER);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return null;
        }

        public ClauseJoinBuilder leftJoin(Class<?> clazz) {
            try {
                return buildSubJoin().leftJoin(clazz);//join(clazz, TYPE_JOIN_INNER);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return null;
        }

        public ClauseJoinBuilder rightJoin(Class<?> clazz) {
            try {
                return buildSubJoin().rightJoin(clazz);//join(clazz, TYPE_JOIN_INNER);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return null;
        }

        public ClauseJoinBuilder fullJoin(Class<?> clazz) {
            try {
                return buildSubJoin().fullJoin(clazz);//join(clazz, TYPE_JOIN_INNER);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return null;
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
            selectionTable += " ON (" + columnJoinName + "=" + name + ") ";
            this.joinSelect.selectionTable = selectionTable;
            return joinSelect;
        }
    }

    public final class SQLiteJoinSelect extends SQLiteSelect {

        SQLiteJoinSelect(SQLite.SQL db, Class<?>... clazz) {
            super(db, clazz);
            this.whereClause = SQLiteSelect.this.whereClause;
            this.whereParams = SQLiteSelect.this.whereParams;
            this.selectionTable = SQLiteSelect.this.selectionTable;
            this.table = SQLiteSelect.this.table;
        }

        public ClauseJoinSelectBuilder where(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildWhereParam(model.getName(), column));
                else
                    whereClause.append(" AND " + buildWhereParam(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, this);
        }

        public ClauseJoinSelectBuilder or(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildWhereParam(model.getName(), column));
                else
                    whereClause.append(" OR " + buildWhereParam(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, this);
        }

        public ClauseJoinSelectBuilder and(Class<?> clazz, String column) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildWhereParam(model.getName(), column));
                else
                    whereClause.append(" AND " + buildWhereParam(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, this);
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
            selectionTable += joinType + "JOIN " + join;
//            if (!TextUtils.isEmpty(on)) {
//                selectionTable += " ON (" + on + ") ";
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SQLiteJoinSelect joinSelection = new SQLiteJoinSelect(this.sql, this.clazz);
        return new ClauseJoinBuilder(clazz, joinSelection);
    }

    public SQLiteSelectLimit limit(int limit) {
        return limit(-1, limit);
    }

    public SQLiteSelectLimit limit(int offset, int limit) {
        String limitS;
        if (limit < 0) {
            limitS = null;
        } else {
            if (offset < 0) {
                offset = 0;
            }
            limitS = offset + ", " + limit;
        }
        return new SQLiteSelectLimit(limitS);
    }

    public class SQLiteSelectLimit {
        SQLiteSelectLimit(String limitS) {
            SQLiteSelect.this.limit = limitS;
        }

        public <T> void execute(List<T> list) {
            SQLiteSelect.this.execute(list);
        }

        public <T> List<T> execute() {
            return SQLiteSelect.this.execute();
        }

        public <T> List<T> execute(Class<T> clazz) {
            return SQLiteSelect.this.execute(clazz);
        }

        public <T> SQLiteThread<List<T>> executeAsync() {
            return SQLiteSelect.this.executeAsync(-1, -1, null);
        }

        public <T> SQLiteThread<List<T>> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<List<T>> callback) {
            return SQLiteSelect.this.executeAsync(-1, -1, callback);
        }

        public <T> SQLiteThread<List<T>> executeAsync(final int limit, final SQLiteAsyncExecutor.ExecutionCallback<List<T>> callback) {
            return SQLiteSelect.this.executeAsync(-1, limit, callback);
        }

        public <T> SQLiteThread<List<T>> executeAsync(final int offset, final int limit, final SQLiteAsyncExecutor.ExecutionCallback<List<T>> callback) {
            return SQLiteSelect.this.executeAsync(offset, limit, callback);
        }

        public String getStatement() {
            return SQLiteSelect.this.getStatement();
        }
    }
}
