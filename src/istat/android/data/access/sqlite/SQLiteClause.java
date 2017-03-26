package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.text.TextUtils;

import istat.android.data.access.sqlite.interfaces.QueryAble;

abstract class SQLiteClause<Clause extends SQLiteClause<?>> {
    protected SQLite.SQL sql;
    // protected SQLiteDatabase db;
    protected String whereClause = null;
    protected List<String> whereParams = new ArrayList<String>();
    protected String orderBy = null;
    protected String groupBy = null;
    protected String having = null;
    protected String limit = null;
    protected String[] columns;
    protected String table;
    final static int TYPE_CLAUSE_WHERE = 0, TYPE_CLAUSE_AND = 1,
            TYPE_CLAUSE_OR = 2, TYPE_CLAUSE_LIKE = 3;

    protected String getWhereClause() {
        return whereClause;
    }

    protected String getOrderBy() {
        return orderBy;
    }

    protected String getHaving() {
        return having;
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
            orderBy = buildWhereParam(column) + " " + value;
        else
            orderBy += buildWhereParam(column) + " " + value;
        return (Clause) this;
    }

    public Clause groupBy(String... column) {
        for (String cl : column) {
            groupBy(cl);
        }
        return (Clause) this;
    }

    //TODO implements this method
    public Clause having() {


        return (Clause) this;
    }

    public Clause groupBy(String column) {
        if (TextUtils.isEmpty(orderBy))
            orderBy = buildWhereParam(column);
        else
            orderBy += ", " + buildWhereParam(column);
        return (Clause) this;
    }

    public Clause orderBy(String... columns) {
        for (String column : columns) {
            boolean endWithDescOrAsc = column.toLowerCase().matches(".+\\s(desc|asc)$");
            orderBy(column, endWithDescOrAsc ? "" : "ASC");
        }
        return (Clause) this;
    }

    protected static String buildWhereParam(String tableName, String column) {
        if (column.matches(".*\\..*")) {
            return column;
        }
        return tableName + "." + column;
    }

    protected String buildWhereParam(String column) {
        return buildWhereParam(table, column);
    }

    public ClauseBuilder where(String column) {
        if (whereClause == null)
            whereClause = buildWhereParam(column);
        else
            whereClause += " AND " + buildWhereParam(column);
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    public ClauseBuilder or(String column) {
        if (whereClause == null)
            whereClause = buildWhereParam(column);
        else
            whereClause += " OR " + buildWhereParam(column);
        return new ClauseBuilder(TYPE_CLAUSE_OR);
    }

    public ClauseBuilder and(String column) {
        if (whereClause == null)
            whereClause = buildWhereParam(column);
        else
            whereClause += " AND " + buildWhereParam(column);
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    @SuppressWarnings("unchecked")
    public Clause WHERE(SQLiteSelect close) {
        if (whereClause == null) {
            this.whereClause = close.whereClause;
            this.whereParams = close.whereParams;
        } else {
            this.whereClause += " AND " + close.whereClause;
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
        this.whereClause = "(" + this.whereClause + ") OR (" + close.whereClause
                + ")";
        this.whereParams.addAll(close.whereParams);
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause AND(SQLiteSelect close) {
        this.whereClause = "(" + this.whereClause + ") AND (" + close.whereClause
                + ")";
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
        Object obj = null;
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

        public ClauseBuilder(int type) {
            this.type = type;
        }

        public Clause isNULL() {
            whereClause += " IS NULL ";
            return (Clause) SQLiteClause.this;
        }

        public Clause isNOTNULL() {
            whereClause += " IS NOT NULL ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause equalTo(Object value) {
            prepare(value);
            whereClause += " = ? ";
            return (Clause) SQLiteClause.this;
        }

        public Clause in(Object... value) {
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
            prepare("(" + valueIn + ")");
            whereClause += " IN ? ";
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
            whereClause += " >" + (acceptEqual ? "=" : "") + " ? ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause += " <" + (acceptEqual ? "=" : "") + " ? ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause like(Object value) {
            prepare(value);
            whereClause += " like ? ";
            return (Clause) SQLiteClause.this;
        }

        //------------------------------------------------
        @SuppressWarnings("unchecked")
        public Clause equalTo(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause += " = (" + value + ") ";
            return (Clause) SQLiteClause.this;
        }

        public Clause in(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause += " IN (" + value.getSql() + ") ";
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
            whereClause += " >" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(SQLiteSelect value, boolean acceptEqual) {
            whereParams.addAll(value.whereParams);
            whereClause += " <" + (acceptEqual ? "=" : "") + " (" + value.getSql() + ") ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause like(SQLiteSelect value) {
            whereParams.addAll(value.whereParams);
            whereClause += " like (" + value.getSql() + ")";
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

        public HavingBuilder(int type) {
            this.type = type;
        }

//        @SuppressWarnings("unchecked")
//        public Clause groupBy(String... column) {
//            prepare(column);
//            whereClause += " = ? ";
//            return (Clause) SQLiteClause.this;
//        }

        public Clause greatThan(Object value) {
            return greatThan(value, false);
        }

        public Clause lessThan(Object value) {
            return lessThan(value, false);
        }

        @SuppressWarnings("unchecked")
        public Clause greatThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause += " >" + (acceptEqual ? "=" : "") + " ? ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClause += " <" + (acceptEqual ? "=" : "") + " ? ";
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

        @SuppressWarnings("unchecked")
        public Clause like(Object value) {
            prepare(value);
            whereClause += " like ? ";
            return (Clause) SQLiteClause.this;
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

    protected void notifyExecutionSucceed(int type, Object clause, Object result) {

    }

    protected void notifyExecutionFail(Exception e) {

    }

    protected void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }
}
