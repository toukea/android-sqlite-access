package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.text.TextUtils;

import istat.android.data.access.sqlite.interfaces.QueryAble;

abstract class SQLiteClause<Clause extends SQLiteClause<?>> {
    protected SQLite.SQL sql;
    // protected SQLiteDatabase db;
    protected StringBuilder whereClause = null;
    protected List<String> whereParams = new ArrayList<String>();
    List<String> havingWhereParams = new ArrayList<String>();
    protected String orderBy = null;
    protected String groupBy = null;
    protected StringBuilder having = null;
    protected String limit = null;
    protected String[] columns;
    protected String table;
    final static int TYPE_CLAUSE_WHERE = 0,
            TYPE_CLAUSE_AND = 1,
            TYPE_CLAUSE_OR = 2,
            TYPE_CLAUSE_LIKE = 3,
            TYPE_CLAUSE_AND_HAVING = 4,
            TYPE_CLAUSE_OR_HAVING = 5;

    protected String getWhereClause() {
        return whereClause != null ? whereClause.toString() : null;
    }

    protected String getOrderBy() {
        return orderBy;
    }

    protected String getHaving() {
        if (having == null) {
            return null;
        }
        String out = compute(this.having.toString(), havingWhereParams);
        return out;
    }

    protected String getLimit() {
        return limit;
    }

    protected String getGroupBy() {
        return groupBy;
    }

    protected String[] getWhereParams() {
        if (whereParams.size() == 0)
            return null;
        String[] tmp = new String[whereParams.size()];
        int i = 0;
        for (String tmpS : whereParams) {
            tmp[i] = tmpS;
            i++;
        }
        return tmp;
    }

    SQLiteClause(String table, String[] projection, SQLite.SQL sql) {
        this.table = table;
        this.columns = projection;
        this.sql = sql;
    }

    SQLiteClause(Class<?> clazz, SQLite.SQL sql) {
        //this.db = sql.db;
        this.sql = sql;
        QueryAble entity = null;//null;//createModelFromClass(clazz);
        try {
            entity = SQLiteModel.fromClass(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (entity != null) {
            table = entity.getName();
            columns = entity.getColumns();
        }
    }

    @SuppressWarnings("unchecked")
    public Clause orderBy(String column, String value) {
        if (TextUtils.isEmpty(orderBy))
            orderBy = buildRealColumnName(column) + " " + value;
        else
            orderBy += buildRealColumnName(column) + " " + value;
        return (Clause) this;
    }

    public Clause groupBy(String... column) {
        for (String cl : column) {
            groupBy(cl);
        }
        return (Clause) this;
    }

    public Clause groupBy(String column) {
        if (TextUtils.isEmpty(groupBy))
            groupBy = buildRealColumnName(column);
        else
            groupBy += ", " + buildRealColumnName(column);
        return (Clause) this;
    }

    public Clause orderBy(String... columns) {
        for (String column : columns) {
            boolean endWithDescOrAsc = column.toLowerCase().matches(".+\\s(desc|asc)$");
            orderBy(column, endWithDescOrAsc ? "" : "ASC");
        }
        return (Clause) this;
    }

    protected static String buildRealColumnName(String tableName, String column) {
        if (column.matches(".*\\..*")) {
            return column;
        }
        Pattern pattern = Pattern.compile("(\\()(\\w*)(\\))");
        Matcher matcher = pattern.matcher(column);
        if (matcher.matches()) {
            while (matcher.find()) {
                String columnNameOnly = matcher.group(2);
                column = column.replace(columnNameOnly, tableName + "." + columnNameOnly);
            }
            return column;
        } else {
            return tableName + "." + column;
        }
    }

    protected String buildRealColumnName(String column) {
        return buildRealColumnName(table, column);
    }

    public ClauseBuilder where(String column) {
        if (whereClause == null) {
            whereClause = new StringBuilder(buildRealColumnName(column));
        } else
            whereClause.append(" AND " + buildRealColumnName(column));
        return new ClauseBuilder(this.whereClause, this.whereParams, TYPE_CLAUSE_AND);
    }

    public ClauseBuilder or(String column) {
        if (whereClause == null)
            whereClause = new StringBuilder(buildRealColumnName(column));
        else
            whereClause.append(" OR " + buildRealColumnName(column));
        return new ClauseBuilder(this.whereClause, this.whereParams, TYPE_CLAUSE_OR);
    }

    public ClauseBuilder and(String column) {
        if (whereClause == null)
            whereClause = new StringBuilder(buildRealColumnName(column));
        else
            whereClause.append(" AND " + buildRealColumnName(column));
        return new ClauseBuilder(this.whereClause, this.whereParams, TYPE_CLAUSE_AND);
    }

    @SuppressWarnings("unchecked")
    public Clause WHERE(SQLiteSelect close) {
        if (whereClause == null) {
            this.whereClause = close.whereClause;
            this.whereParams = close.whereParams;
        } else {
            this.whereClause.append(" AND " + close.whereClause);
            this.whereParams.addAll(close.whereParams);
        }
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause WHERE(HashMap<String, String> filter) {
        applyFilter(filter);
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause OR(SQLiteSelect close) {
        this.whereClause.append("(" + this.whereClause + ") OR (" + close.whereClause
                + ")");
        this.whereParams.addAll(close.whereParams);
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause AND(SQLiteSelect close) {
        this.whereClause.append("(" + this.whereClause + ") AND (" + close.whereClause
                + ")");
        this.whereParams.addAll(close.whereParams);
        return (Clause) this;
    }

    protected abstract Object onExecute(SQLiteDatabase db);

    protected static QueryAble createFromCursor(
            Class<? extends QueryAble> clazz, Cursor c) {
        QueryAble instance = createModelFromClass(clazz);
        if (instance != null) {
            instance.fillFromCursor(c);
        }
        return instance;
    }

    protected static QueryAble createModelFromClass(
            Class<? extends QueryAble> clazz) {
        String className = clazz + "";
        className = className.substring(6, className.length()).trim();
        Object obj;
        try {
            obj = Class.forName(className).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            try {

                Class<?> cLass = Class.forName(className);
                obj = cLass.getConstructor(JSONObject.class).newInstance(
                        new JSONObject());
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        if (obj != null) {
            try {
                if (obj instanceof QueryAble) {
                    QueryAble jsonEntity = (QueryAble) obj;
                    return jsonEntity;
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public class ClauseBuilder {
        int type = 0;
        StringBuilder whereClause;
        List<String> whereParams;

        public ClauseBuilder(StringBuilder whereClause, List<String> whereParams, int type) {
            this.type = type;
            this.whereClause = whereClause;
            this.whereParams = whereParams;
        }

        public Clause isNULL() {
            whereClause.append(" IS NULL ");
            return (Clause) SQLiteClause.this;
        }

        public Clause isNOTNULL() {
            whereClause.append(" IS NOT NULL ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause equalTo(Object value) {
            prepare(value);
            whereClause.append(" = ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause notEqualTo(Object value) {
            prepare(value);
            whereClause.append(" != ? ");
            return (Clause) SQLiteClause.this;
        }

        public Clause notIn(Object... value) {
            return in(false, value);
        }

        public Clause in(Object... value) {
            return in(false, value);
        }

        private Clause in(boolean truth, Object[] value) {
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
            return (Clause) SQLiteClause.this;
        }

        @Deprecated
        public Clause equal(Object value) {
            return equalTo(value);
        }

        public Clause greatThan(Object value) {
            return greatThan(value, false);
        }

        public Clause lessThan(Object value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public Clause greatThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause like(Object value) {
            prepare(value);
            whereClause.append(" like ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause notLike(Object value) {
            prepare(value);
            whereClause.append(" NOT like ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause between(Object value, Object value2) {
            prepare(value);
            prepare(value2);
            whereClause.append(" BETWEEN ? AND ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause notBetween(Object value, Object value2) {
            prepare(value);
            prepare(value2);
            whereClause.append(" NOT BETWEEN ? AND ? ");
            return (Clause) SQLiteClause.this;
        }

        //------------------------------------------------
        @SuppressWarnings("unchecked")
        public Clause equalTo(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" = (" + value + ") ");
            return (Clause) SQLiteClause.this;
        }

        public Clause notEqualTo(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" != (" + value + ") ");
            return (Clause) SQLiteClause.this;
        }

        public Clause notIn(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" NOT IN (" + value.getSql() + ") ");
            return (Clause) SQLiteClause.this;
        }

        public Clause in(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" IN (" + value.getSql() + ") ");
            return (Clause) SQLiteClause.this;
        }

        public Clause greatThan(SQLiteSelect value) {
            return greatThan(value, false);
        }

        public Clause lessThan(SQLiteSelect value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public Clause greatThan(SQLiteSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(SQLiteSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause like(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" like (" + value.getSql() + ")");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause notLike(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" NOT like (" + value.getSql() + ")");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause between(SQLiteSelect value, SQLiteSelect value2) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" BETWEEN (" + value.getSql() + ") AND (" + value2.getSql() + ")");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause notBetween(SQLiteSelect value, SQLiteSelect value2) {
            whereParams.addAll(value.whereParams);
            whereClause.append(" BETWEEN (" + value.getSql() + ") AND (" + value2.getSql() + ")");
            return (Clause) SQLiteClause.this;
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

    //TODO build end of query. GroupBy orderBy, Having with execute
    public class HavingBuilder {
        int type = 0;
        String having;

        public HavingBuilder(String having, int type) {
            this.having = having;
            this.type = type;
        }

        public Clause greatThan(Object value) {
            return greatThan(value, false);
        }

        public Clause lessThan(Object value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public Clause greatThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" >" + (acceptEqual ? "=" : "") + " ? ");
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause.append(" <" + (acceptEqual ? "=" : "") + " ? ");
            return (Clause) SQLiteClause.this;
        }

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

    private void applyFilter(HashMap<String, String> filter) {
        Iterator<String> keySet = filter.keySet().iterator();
        while (keySet.hasNext()) {
            String tmp = keySet.next();
            Object obj = filter.get(tmp);
            if (obj != null) {
                String value = obj.toString();
                where(tmp).equalTo(value);
            }
        }
    }

    public abstract String getStatement();

    protected void notifyExecuting() {

    }

    protected void notifyExecutionSucceed(int type, Object clause, Object result) {

    }

    protected void notifyExecutionFail(Exception e) {

    }

    protected void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }

    String compute(String out, List<String> whereParams) {
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
