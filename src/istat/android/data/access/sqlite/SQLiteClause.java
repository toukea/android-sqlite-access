package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

abstract class SQLiteClause<Clause extends SQLiteClause<?>> {
    protected SQLiteDatabase db;
    protected String whereClause = null;
    protected List<String> whereParams = new ArrayList<String>();
    protected String orderBy = null;
    protected String groupBy = null;
    protected String having = null;
    protected String[] projection;
    protected String table;
    final static int TYPE_CLAUSE_WHERE = 0, TYPE_CLAUSE_AND = 1,
            TYPE_CLAUSE_OR = 2, TYPE_CLAUSE_LIKE = 3;

    protected String getWhereClause() {
        return whereClause;
    }

    protected String getOrderBy() {
        return orderBy;
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

    protected SQLiteClause(String table, String[] projection, SQLiteDatabase db) {
        this.table = table;
        this.projection = projection;
    }

    protected SQLiteClause(SQLiteDatabase db) {

    }

    protected SQLiteClause(Class<?> clazz, SQLiteDatabase db) {
        this.db = db;
        QueryAble entity = null;//null;//createModelFromClass(clazz);
        try {
            entity = SQLiteModel.fromClass(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (entity != null) {
            table = entity.getName();
            projection = entity.getProjections();
        }
    }

    @SuppressWarnings("unchecked")
    public Clause orderBy(String column, String value) {
        if (orderBy == null)
            orderBy = this.table + "." + column + " " + value;
        else
            orderBy += this.table + "." + column + " " + value;
        return (Clause) this;
    }

    public Clause orderBy(String... columns) {
        for (String column : columns) {
            boolean endWithDescOrAsc = column.toLowerCase().matches(".+\\s(desc|asc)$");
            orderBy(column, endWithDescOrAsc ? "" : "DESC");
        }
        return (Clause) this;
    }

    public ClauseBuilder where(String column) {
        if (whereClause == null)
            whereClause = this.table + "." + column;
        else
            whereClause += " AND " + this.table + "." + column;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    public ClauseBuilder or(String column) {
        if (whereClause == null)
            whereClause = this.table + "." + column;
        else
            whereClause += " OR " + this.table + "." + column;
        return new ClauseBuilder(TYPE_CLAUSE_OR);
    }

    public ClauseBuilder and(String column) {
        if (whereClause == null)
            whereClause = this.table + "." + column;
        else
            whereClause += " AND " + this.table + "." + column;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    @SuppressWarnings("unchecked")
    public Clause WHERE(Clause close) {
        this.whereClause = close.whereClause;
        this.whereParams = close.whereParams;
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause WHERE(HashMap<String, String> filter) {
        applyFilter(filter);
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause OR(Clause close) {
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
                    QueryAble jsonentity = (QueryAble) obj;
                    return jsonentity;
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

        @SuppressWarnings("unchecked")
        public Clause equal(Object value) {
            prepare(value);
            whereClause += " = ? ";
            return (Clause) SQLiteClause.this;
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

    //TODO build end of query. GroupBy orderBy, Having with execute
    public class ClauseSubBuilder {
        int type = 0;

        public ClauseSubBuilder(int type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public Clause groupBy(String... column) {
            prepare(column);
            whereClause += " = ? ";
            return (Clause) SQLiteClause.this;
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
                where(tmp).equal(value);
            }
        }
    }

    protected String getSQL() {
        String out = "SELECT * FROM " + table + " WHERE " + whereClause.trim();
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
