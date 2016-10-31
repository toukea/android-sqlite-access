package istat.android.data.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class SQLiteClause<Clause extends SQLiteClause<?>> {
    protected SQLiteDatabase db;
    protected String whereClose = null;
    protected List<String> whereParams = new ArrayList<String>();
    protected String orderBy = null;
    protected String[] projection;
    protected String table;
    final static int TYPE_CLAUSE_WHERE = 0, TYPE_CLAUSE_AND = 1,
            TYPE_CLAUSE_OR = 2, TYPE_CLAUSE_LIKE = 3;

    protected String getWhereClose() {
        return whereClose;
    }

    protected String getOrderBy() {
        return orderBy;
    }

    protected String[] getWhereParams() {
        if (whereParams.size() == 0)
            return null;
        String[] tmp = new String[whereParams.size()];
        int i = 0;
        for (String tmps : whereParams) {
            tmp[i] = tmps;
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
        QueryAble entity = null;//createEntityInstance(clazz);
        if (entity != null) {
            table = entity.getEntityName();
            projection = entity.getEntityFieldNames();
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

    public ClauseBuilder where(String column) {
        if (whereClose == null)
            whereClose = this.table + "." + column;
        else
            whereClose += " AND " + this.table + "." + column;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    public ClauseBuilder where(Class<? extends QueryAble> clazz, String column) {
        String table = this.table;
        if (clazz != null) {
            QueryAble entity = createEntityInstance(clazz);
            if (entity != null) {
                table = entity.getEntityName();
            }
        }
        if (whereClose == null) {
            whereClose = table + "." + column;
        } else {
            whereClose += " AND " + table + "." + column;
        }
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    public ClauseBuilder or(String column) {
        if (whereClose == null)
            whereClose = this.table + "." + column;
        else
            whereClose += " OR " + this.table + "." + column;
        return new ClauseBuilder(TYPE_CLAUSE_OR);
    }

    public ClauseBuilder and(String column) {
        if (whereClose == null)
            whereClose = this.table + "." + column;
        else
            whereClose += " AND " + this.table + "." + column;
        return new ClauseBuilder(TYPE_CLAUSE_AND);
    }

    @SuppressWarnings("unchecked")
    public Clause WHERE(Clause close) {
        this.whereClose = close.whereClose;
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
        this.whereClose = "(" + this.whereClose + ") OR (" + close.whereClose
                + ")";
        this.whereParams.addAll(close.whereParams);
        return (Clause) this;
    }

    @SuppressWarnings("unchecked")
    public Clause AND(SQLiteSelect close) {
        this.whereClose = "(" + this.whereClose + ") AND (" + close.whereClose
                + ")";
        this.whereParams.addAll(close.whereParams);
        return (Clause) this;
    }

    protected abstract Object onExecute(SQLiteDatabase db);

    protected static QueryAble createFromCursor(
            Class<? extends QueryAble> clazz, Cursor c) {
        QueryAble instance = createEntityInstance(clazz);
        if (instance != null) {
            instance.fillFromCursor(c);
        }
        return instance;
    }

    protected static QueryAble createEntityInstance(
            Class<? extends QueryAble> clazz) {
        String className = clazz + "";
        className = className.substring(6, className.length()).trim();
        Object obj = null;
        try {
            obj = Class.forName(className).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            try {

                Class<?> clazzs = Class.forName(className);
                obj = clazzs.getConstructor(JSONObject.class).newInstance(
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
            whereClose += " = ? ";
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
            whereClose += " >" + (acceptEqual ? "=" : "") + " ? ";
            return (Clause) SQLiteClause.this;
        }

        @SuppressWarnings("unchecked")
        public Clause lessThan(Object value, boolean acceptEqual) {
            prepare(value);
            whereClose += " <" + (acceptEqual ? "=" : "") + " ? ";
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
            whereClose += " like ? ";
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
        String out = "SELECT * FROM " + table + " WHERE " + whereClose.trim();
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
