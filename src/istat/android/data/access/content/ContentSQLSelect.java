package istat.android.data.access.content;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

import com.google.gson.Gson;

import istat.android.data.access.content.interfaces.QueryAble;
import istat.android.data.access.content.interfaces.SelectionExecutable;
import istat.android.data.access.content.utils.ContentSQLAsyncExecutor;
import istat.android.data.access.content.utils.ContentSQLThread;

public class ContentSQLSelect extends ContentSQLClause<ContentSQLSelect> implements SelectionExecutable {
    public final static String ORDER_BY_DESC = "DESC", ORDER_BY_ASC = "ASC";
    public final static int TYPE = 0;
    Class<?> clazz;
    String selectionTable;
    boolean distinct = false;


    ContentSQLSelect(ContentSQL.SQL db, Class<?>... clazz) {
        super(clazz[0], db);
        this.clazz = clazz[0];
        this.selectionTable = this.table;
    }

    public ContentSQLJoinSelect joinOn(Class<?> clazz, String on) {
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
        return new ContentSQLJoinSelect(this.sql, this.clazz);
    }

    public ContentSQLJoinSelect joinOn(String joinTable, String on) {
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
        return new ContentSQLJoinSelect(this.sql, this.clazz);
    }

//    protected ContentSQLSelect distinct() {
//        return distinct(true);
//    }

    protected ContentSQLSelect distinct(boolean enable) {
        distinct = enable;
        return this;
    }

    private QueryAble createQueryAble(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        //TODO make it better
        return ContentSQLModel.fromClass(clazz);
    }

    @Override
    protected Cursor onExecute(ContentResolver db) {
        notifyExecuting();
        String[] smartColumns = new String[columns.length];
        String tableName = table;
        for (int i = 0; i < columns.length; i++) {
//            smartColumns[i] = tableName + "." + columns[i];
            smartColumns[i] = buildRealColumnName(tableName, columns[i]);
        }
        return db.query(sql.dataUri, distinct, selectionTable, smartColumns, getWhereClause(), getWhereParams(),
                getGroupBy(), getHaving(), getOrderBy(), getLimit());
    }


    public int count() {
        Cursor c = onExecute(sql.getContentResolver());
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

    public <T> T executeLimit1() {
        List<T> results = execute(1);
        return results.isEmpty() ? null : results.get(0);
    }

    public Cursor getCursor() {
        return onExecute(this.sql.getContentResolver());
    }

    @Override
    public ContentSQLModel getSingleResult() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            return limit(1).getResults().get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public List<ContentSQLModel> getResults() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Cursor c = getCursor();
        List<ContentSQLModel> list = new ArrayList<ContentSQLModel>();
        while (c.moveToNext()) {
            ContentSQLModel model = ContentSQLModel.fromClass(this.clazz,
                    sql.getSerializer(this.clazz),
                    sql.getContentValueHandler(this.clazz));
            model.fillFromCursor(c);
            list.add(model);
        }
        return list;
    }

    public <T> void execute(List<T> list) {
        execute(list, this.clazz);
    }

    public <T> List<T> execute(Class<T> clazz) {
        List<T> list = new ArrayList<T>();
        execute(list, clazz);
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(List<T> list, Class<?> clazz) {
        if (list == null) {
            return;
        }
        try {
            Cursor c = onExecute(sql.getContentResolver());
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    T model = (T) ContentSQLModel.cursorAsClass(c, clazz, sql.getSerializer(clazz), sql.getCursorReader(clazz));
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

    //-----------------------------------------------------
    @SuppressWarnings("unchecked")
    public <T> void execute(List<T> list, Class<T> clazz, String uniqueColumn) {
        if (list == null) {
            return;
        }
        try {
            Cursor c = onExecute(sql.getContentResolver());
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    String serializedValue = c.getString(c.getColumnIndex(uniqueColumn));
                    //TODO you can't call Gson there. :-(
                    T model = new Gson().fromJson(serializedValue, clazz);
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

    @SuppressWarnings("unchecked")
    public <T> List<T> execute(Class<T> clazz, String uniqueColumn) {
        List<T> list = new ArrayList();
        execute(list, clazz, uniqueColumn);
        return list;
    }


    public <T> ContentSQLThread<List<T>> executeAsync() {
        return executeAsync(-1, -1, null);
    }

    public <T> ContentSQLThread<List<T>> executeAsync(final ContentSQLAsyncExecutor.SelectionCallback<T> callback) {
        return executeAsync(-1, -1, callback);
    }

    public <T> ContentSQLThread<T> executeAsync(final ContentSQLAsyncExecutor.ExecutionCallback<T> callback) {
        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
        return asyncExecutor.execute(this, callback);
    }

    public <T> ContentSQLThread<List<T>> executeAsync(final int limit, final ContentSQLAsyncExecutor.SelectionCallback<T> callback) {
        return executeAsync(-1, limit, callback);
    }

    public <T> ContentSQLThread<List<T>> executeAsync(final int offset, final int limit, final ContentSQLAsyncExecutor.SelectionCallback<T> callback) {
        ContentSQLAsyncExecutor asyncExecutor = new ContentSQLAsyncExecutor();
        return asyncExecutor.execute(this, offset, limit, callback);
    }

//    //TODO check if 'selectionTable' or 'table'
//    public ClauseBuilder AND_SELECT(ContentSQLSelect clause) {
//        this.whereClause = "(SELECT * FROM " + selectionTable + " WHERE "
//                + clause.whereClause + ")";
//        this.whereParams = clause.whereParams;
//        return new ClauseBuilder(TYPE_CLAUSE_AND);
//    }
//
//    public ClauseBuilder OR_SELECT(ContentSQLSelect clause) {
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
        String whereClause = getWhereClause();
        String having = getHaving();
        String orderBy = getOrderBy();
        String groupBy = getGroupBy();
        String limit = getLimit();
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE " + whereClause.trim();
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
            ContentSQLModel entity = ContentSQLModel.fromClass(clazz);
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
        String whereClause = getWhereClause();
        String having = getHaving();
        String orderBy = getOrderBy();
        String groupBy = getGroupBy();
        String limit = getLimit();
        if (!TextUtils.isEmpty(whereClause)) {
            out += " WHERE " + whereClause.trim();
        }
        String sql = compute(out, this.whereParams);
        if (!TextUtils.isEmpty(orderBy)) {
            sql += " ORDER BY " + orderBy;
        }
        if (!TextUtils.isEmpty(groupBy)) {
            sql += " GROUP BY " + groupBy;
        }

        if (!TextUtils.isEmpty(having)) {
            sql += " HAVING " + having;
        }
        if (!TextUtils.isEmpty(limit)) {
            sql += " LIMIT " + limit;
        }
        return sql;
    }

    public class ClauseJoinSelectBuilder {
        int type = 0;
        ContentSQLJoinSelect selectClause;

        ClauseJoinSelectBuilder(int type, ContentSQLJoinSelect selectClause) {
            this.type = type;
            this.selectClause = selectClause;
        }

        public ContentSQLJoinSelect isNULL() {
            whereClause.append(" IS NULL ");
            return selectClause;
        }

        public ContentSQLJoinSelect isNOTNULL() {
            whereClause.append(" IS NOT NULL ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect equalTo(Object value) {
            prepare(value);
            whereClause.append(" = ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect notEqualTo(Object value) {
            prepare(value);
            whereClause.append(" = ? ");
            return selectClause;
        }

        public <T> ContentSQLJoinSelect notIn(T... value) {
            return in(false, value);
        }

        public <T> ContentSQLJoinSelect in(T... value) {
            return in(true, value);
        }

        private <T> ContentSQLJoinSelect in(boolean truth, T[] value) {
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

        public ContentSQLJoinSelect greatThan(Object value) {
            return greatThan(value, false);
        }

        public ContentSQLJoinSelect lessThan(Object value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect greatThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect like(Object value) {
            prepare(value);
            whereClause.append(" like ? ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect notLike(Object value) {
            prepare(value);
            whereClause.append(" NOT like ? ");
            return selectClause;
        }

        //------------------------------------------------
        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect equalTo(ContentSQLSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" = (" + value + ") ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect notEqualTo(ContentSQLSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" = (" + value + ") ");
            return selectClause;
        }

        public ContentSQLJoinSelect in(ContentSQLSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" IN (" + value.getSql() + ") ");
            return selectClause;
        }

        public ContentSQLJoinSelect notIn(ContentSQLSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" NOT IN (" + value.getSql() + ") ");
            return selectClause;
        }

        public ContentSQLJoinSelect greatThan(ContentSQLSelect value) {
            return greatThan(value, false);
        }

        public ContentSQLJoinSelect lessThan(ContentSQLSelect value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect greatThan(ContentSQLSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect lessThan(ContentSQLSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect like(ContentSQLSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" like (" + value.getSql() + ")");
            return selectClause;
        }

        @SuppressWarnings("unchecked")
        public ContentSQLJoinSelect notLike(ContentSQLSelect value) {
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
        ContentSQLJoinSelect joinSelect;
        Class<?> clazz;

        ClauseJoinBuilder(Class<?> clazz, ContentSQLJoinSelect selection) {
            this.clazz = clazz;
            this.joinSelect = selection;
        }

        public ContentSQLJoinSelect where1() {
            return joinSelect;
        }

        public ClauseSubJoinBuilder on(Class<?> clazz, String name) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(clazz);
                name = buildRealColumnName(model.getName(), name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseSubJoinBuilder(joinSelect, name);
        }

        private ContentSQLJoinSelect buildSubJoin() throws IllegalAccessException, InstantiationException {
            Class<?> selectionClass = joinSelect.clazz;
            Class<?> joinClass = this.clazz;
            ContentSQLModel selectionModel = ContentSQLModel.fromClass(selectionClass);
            ContentSQLModel joinModel = ContentSQLModel.fromClass(selectionClass);
            Field[] fields = selectionModel.getNestedTableFields();
            String nestedPrimaryKey = joinModel.getPrimaryKeyName();
            String foreignKey = selectionModel.getPrimaryKeyName();
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
                ContentSQLModel model = ContentSQLModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildRealColumnName(model.getName(), column));
                else
                    whereClause.append(" AND " + buildRealColumnName(model.getName(), column));
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
        ContentSQLJoinSelect joinSelect;
        String columnJoinName;

        ClauseSubJoinBuilder(ContentSQLJoinSelect joinSelect, String name) {
            this.joinSelect = joinSelect;
            this.columnJoinName = name;

        }

        public ContentSQLJoinSelect equalTo(Class<?> clazz, String name) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(clazz);
                name = buildRealColumnName(model.getName(), name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            selectionTable += " ON (" + columnJoinName + "=" + name + ") ";
            this.joinSelect.selectionTable = selectionTable;
            return joinSelect;
        }
    }

    public final class ContentSQLJoinSelect extends ContentSQLSelect {

        ContentSQLJoinSelect(ContentSQL.SQL db, Class<?>... clazz) {
            super(db, clazz);
            this.whereClause = ContentSQLSelect.this.whereClause;
            this.whereParams = ContentSQLSelect.this.whereParams;
            this.selectionTable = ContentSQLSelect.this.selectionTable;
            this.table = ContentSQLSelect.this.table;
        }

        public ClauseJoinSelectBuilder where(Class<?> clazz, String column) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildRealColumnName(model.getName(), column));
                else
                    whereClause.append(" AND " + buildRealColumnName(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, this);
        }

        public ClauseJoinSelectBuilder or(Class<?> clazz, String column) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildRealColumnName(model.getName(), column));
                else
                    whereClause.append(" OR " + buildRealColumnName(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, this);
        }

        public ClauseJoinSelectBuilder and(Class<?> clazz, String column) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(clazz);
                if (whereClause == null)
                    whereClause = new StringBuilder(buildRealColumnName(model.getName(), column));
                else
                    whereClause.append(" AND " + buildRealColumnName(model.getName(), column));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ClauseJoinSelectBuilder(TYPE_CLAUSE_AND, this);
        }

        ClauseBuilder internalHaving(String or_and, Class cLass, String having) {
            String table = this.selectionTable;
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                table = model.getName();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (this.having == null)
                this.having = new StringBuilder(buildRealColumnName(table, having));
            else
                this.having.append(" " + or_and + " " + buildRealColumnName(table, having));
            ClauseBuilder builder = new ClauseBuilder(this.having, havingWhereParams, TYPE_CLAUSE_AND_HAVING);
            return builder;
        }

        ClauseBuilder internalHaving(String or_and, Class cLass, String function, String having) {
            String table = this.selectionTable;
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                table = model.getName();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            String value = function + "(" + buildRealColumnName(table, having) + ")";
            if (this.having == null)
                this.having = new StringBuilder(value);
            else
                this.having.append(" " + or_and + " " + value);
            ClauseBuilder builder = new ClauseBuilder(this.having, havingWhereParams, TYPE_CLAUSE_AND_HAVING);
            return builder;
        }

        ClauseBuilder internalHaving(String or_and, ContentSQLFunction function) {
            throw new RuntimeException("Not yet implemented");
        }

        public ClauseBuilder having(Class cLass, String having) {
            return internalHaving("AND", cLass, having);
        }

        public ClauseBuilder andHaving(Class cLass, String having) {
            return internalHaving("AND", cLass, having);
        }

        public ClauseBuilder orHaving(Class cLass, String having) {
            return internalHaving("OR", cLass, having);
        }

        public ClauseBuilder having(Class cLass, String function, String having) {
            return internalHaving("AND", cLass, function, having);
        }

        public ClauseBuilder andHaving(Class cLass, String function, String having) {
            return internalHaving("AND", cLass, function, having);
        }

        public ClauseBuilder orHaving(Class cLass, String function, String having) {
            return internalHaving("OR", cLass, function, having);
        }

        public ClauseBuilder having(ContentSQLFunction function) {
            return internalHaving("AND", function);
        }

        public ClauseBuilder andHaving(ContentSQLFunction function) {
            return internalHaving("AND", function);
        }

        public ClauseBuilder orHaving(ContentSQLFunction function) {
            return internalHaving("OR", function);
        }

        public ContentSQLJoinSelect groupBy(Class<?> cLass, String column) {
            String table = this.selectionTable;
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                table = model.getName();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(groupBy))
                groupBy = buildRealColumnName(table, column);
            else
                groupBy += ", " + buildRealColumnName(table, column);
            return this;
        }

        public ContentSQLJoinSelect orderBy(Class<?> cLass, String column) {
            return orderBy(cLass, column, ORDER_BY_ASC);
        }

        public ContentSQLJoinSelect orderBy(Class<?> cLass, String column, String direction) {
            direction = " " + direction;
            String table = this.selectionTable;
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                table = model.getName();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            String realColumnName = buildRealColumnName(table, column);
            if (TextUtils.isEmpty(orderBy)) {
                orderBy = realColumnName;
            } else {
                orderBy += realColumnName;
            }
            if (!TextUtils.isEmpty(direction)) {
                orderBy += direction;
            }
            return this;
        }


        /*
        there has 2 table in Selection clause. i can't myself choose what table is right to use. So i just put column name
         */
        @Override
        protected String buildRealColumnName(String column) {
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
            selectionTable += joinType + " JOIN " + join;
//            if (!TextUtils.isEmpty(on)) {
//                selectionTable += " ON (" + on + ") ";
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ContentSQLJoinSelect joinSelection = new ContentSQLJoinSelect(this.sql, this.clazz);
        return new ClauseJoinBuilder(clazz, joinSelection);
    }

    public SelectionExecutable limit(int limit) {
        return limit(-1, limit);
    }

    public SelectionExecutable limit(int offset, int limit) {
        String limitS;
        if (limit < 0) {
            limitS = null;
        } else {
            if (offset < 0) {
                offset = 0;
            }
            limitS = offset + ", " + limit;
        }
        this.limit = limitS;
        return this;
    }

    ClauseBuilder internalHaving(String or_and, String having) {
        if (this.having == null)
            this.having = new StringBuilder(buildRealColumnName(having));
        else
            this.having.append(" " + or_and + " " + buildRealColumnName(having));
        ClauseBuilder builder = new ClauseBuilder(this.having, havingWhereParams, TYPE_CLAUSE_AND_HAVING);
        return builder;
    }

    ClauseBuilder internalHaving(String or_and, String function, String having) {
        String value = function + "(" + buildRealColumnName(having) + ")";
        if (this.having == null)
            this.having = new StringBuilder(value);
        else
            this.having.append(" " + or_and + " " + value);
        ClauseBuilder builder = new ClauseBuilder(this.having, havingWhereParams, TYPE_CLAUSE_AND_HAVING);
        return builder;
    }

    ClauseBuilder internalHaving(String or_and, ContentSQLFunction function) {
        throw new RuntimeException("Not yet implemented");
        // return new ClauseBuilder(this.having, new ArrayList<String>(), TYPE_CLAUSE_AND);
    }

    public ClauseBuilder having(String having) {
        return internalHaving("AND", having);
    }

    public ClauseBuilder andHaving(String having) {
        return internalHaving("AND", having);
    }

    public ClauseBuilder orHaving(String having) {
        return internalHaving("OR", having);
    }

    public ClauseBuilder having(String function, String having) {
        return internalHaving("AND", function, having);
    }

    public ClauseBuilder andHaving(String function, String having) {
        return internalHaving("AND", function, having);
    }

    public ClauseBuilder orHaving(String function, String having) {
        return internalHaving("OR", function, having);
    }

    public ClauseBuilder having(ContentSQLFunction function) {
        return internalHaving("AND", function);
    }

    public ClauseBuilder andHaving(ContentSQLFunction function) {
        return internalHaving("AND", function);
    }

    public ClauseBuilder orHaving(ContentSQLFunction function) {
        return internalHaving("OR", function);
    }

    @Override
    public <T> List<T> fetch() {
        List<T> list = new ArrayList();
        fetch(list, this.clazz);
        return list;
    }

    @Override
    public <T> List<T> fetch(Class<T> clazz) {
        List<T> list = new ArrayList();
        fetch(list, clazz);
        return list;
    }

    @Override
    public <T> void fetch(List<T> list, Class<?> fetchedClass) {
        if (list == null) {
            return;
        }
        if (this.columns == null) {
            throw new SQLiteException("column to select can't be null.");
        }
        if (this.columns.length > 1) {
            throw new SQLiteException("when you use fetch you have to select only one colum. current column selection is:" + Arrays.asList(this.columns));
        }
        try {
            Cursor c = onExecute(sql.getContentResolver());
            if (c.getCount() > 0) {
                String column = columns[0];
                ContentSQLModel selectionModel = ContentSQLModel.fromClass(this.clazz);
                while (c.moveToNext()) {
                    ContentSQLModel.Serializer<T> serializer = sql.getSerializer(fetchedClass);
                    T model = (T) serializer.onDeSerialize(c.getString(c.getColumnIndex(column)), selectionModel.getField(column));
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
}
