package istat.android.data.access.sqlite;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import istat.android.data.access.sqlite.utils.SQLiteAsyncExecutor;
import istat.android.data.access.sqlite.utils.SQLiteThread;

public final class SQLiteInsertSelection extends SQLiteClause<SQLiteInsertSelection> {
    private final SQLiteModel targetModel;
    private final LinkedHashMap<String, Projection> projections = new LinkedHashMap<String, Projection>();
    private final String sourceTable;
    private final boolean sourceDistinct;

    SQLiteInsertSelection(Class<?> tableClass, SQLite.SQL sql, SQLiteSelect selection) {
        super(tableClass, sql);
        try {
            this.targetModel = SQLiteModel.fromClass(tableClass);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to resolve model for " + tableClass, e);
        }
        this.sourceTable = selection.selectionTable;
        this.sourceDistinct = selection.distinct;
        copySelectionState(selection);
    }

    private void copySelectionState(SQLiteSelect selection) {
        if (selection.columns != null) {
            this.columns = selection.columns.clone();
        } else {
            this.columns = new String[0];
        }
        this.whereClause = selection.whereClause != null ? new StringBuilder(selection.whereClause.toString()) : null;
        this.whereParams = new ArrayList<String>(selection.whereParams);
        this.whereParamValues = new ArrayList<Object>(selection.whereParamValues);
        this.having = selection.having != null ? new StringBuilder(selection.having.toString()) : null;
        this.havingWhereParams = new ArrayList<String>(selection.havingWhereParams);
        this.havingWhereParamValues = new ArrayList<Object>(selection.havingWhereParamValues);
        this.orderBy = selection.orderBy;
        this.groupBy = selection.groupBy;
        this.limit = selection.limit;
        this.tableAliases.putAll(selection.tableAliases);
        for (String column : this.columns) {
            projections.put(column, new Projection(column, column));
        }
    }

    public SQLiteInsertSelection set(String column, Object value) {
        Projection projection = ensureProjection(column);
        projection.expression = "?";
        projection.bindArg = value;
        updateColumns();
        return this;
    }

    public SQLiteInsertSelection setAs(Object entity) {
        if (entity == null) {
            return this;
        }
        try {
            SQLiteModel model = SQLiteModel.fromObject(entity,
                    sql.getSerializer(entity.getClass()),
                    sql.getContentValueHandler(entity.getClass()));
            for (Map.Entry<String, Object> entry : model.toHashMap().entrySet()) {
                if (targetModel.hasColumn(entry.getKey())) {
                    set(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteInsertSelection setExpression(String column, String rawSql) {
        Projection projection = ensureProjection(column);
        projection.expression = rawSql;
        projection.bindArg = null;
        updateColumns();
        return this;
    }

    private Projection ensureProjection(String column) {
        Projection projection = projections.get(column);
        if (projection == null) {
            projection = new Projection(column, column);
            projections.put(column, projection);
        }
        return projection;
    }

    private void updateColumns() {
        this.columns = projections.keySet().toArray(new String[0]);
    }

    @Override
    protected Object onExecute(SQLiteDatabase db) {
        notifyExecuting();
        String sqlStatement = buildInsertSelectSql();
        Object[] bindArgs = buildBindArgsArray();
        if (bindArgs.length > 0) {
            db.execSQL(sqlStatement, bindArgs);
        } else {
            db.execSQL(sqlStatement);
        }
        long changes = DatabaseUtils.longForQuery(db, "SELECT changes()", null);
        return (int) changes;
    }

    public int execute() {
        try {
            return (Integer) onExecute(sql.db);
        } finally {
            notifyExecuted();
        }
    }

    public SQLiteThread<Integer> executeAsync() {
        return executeAsync(null);
    }

    public SQLiteThread<Integer> executeAsync(final SQLiteAsyncExecutor.ExecutionCallback<Integer> callback) {
        SQLiteAsyncExecutor executor = new SQLiteAsyncExecutor();
        return executor.execute(this, callback);
    }

    @Override
    public String getStatement() {
        String sqlStatement = buildInsertSelectSql();
        List<Object> rawArgs = buildBindArgs();
        List<String> bindArgs = new ArrayList<String>(rawArgs.size());
        for (Object arg : rawArgs) {
            bindArgs.add(arg == null ? null : String.valueOf(arg));
        }
        return compute(sqlStatement, bindArgs, rawArgs);
    }

    @Override
    protected void notifyExecuting() {
        super.notifyExecuting();
    }

    private String buildInsertSelectSql() {
        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ");
        builder.append(targetModel.getName());
        if (!projections.isEmpty()) {
            builder.append(" (");
            int index = 0;
            for (Projection projection : projections.values()) {
                builder.append(projection.column);
                if (index < projections.size() - 1) {
                    builder.append(", ");
                }
                index++;
            }
            builder.append(") ");
        }
        builder.append(buildSelectClause());
        return builder.toString();
    }

    private String buildSelectClause() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");
        if (sourceDistinct) {
            builder.append("DISTINCT ");
        }
        int index = 0;
        for (Projection projection : projections.values()) {
            builder.append(projection.expression);
            if (index < projections.size() - 1) {
                builder.append(", ");
            }
            index++;
        }
        builder.append(" FROM ");
        builder.append(sourceTable);
        if (whereClause != null && whereClause.length() > 0) {
            builder.append(" WHERE ");
            builder.append(whereClause.toString().trim());
        }
        if (!TextUtils.isEmpty(groupBy)) {
            builder.append(" GROUP BY ");
            builder.append(groupBy);
        }
        if (having != null && having.length() > 0) {
            builder.append(" HAVING ");
            builder.append(having.toString().trim());
        }
        if (!TextUtils.isEmpty(orderBy)) {
            builder.append(" ORDER BY ");
            builder.append(orderBy);
        }
        if (!TextUtils.isEmpty(limit)) {
            builder.append(" LIMIT ");
            builder.append(limit);
        }
        return builder.toString();
    }

    private List<Object> buildBindArgs() {
        List<Object> args = new ArrayList<Object>();
        for (Projection projection : projections.values()) {
            if ("?".equals(projection.expression)) {
                args.add(projection.bindArg);
            }
        }
        args.addAll(whereParamValues);
        args.addAll(havingWhereParamValues);
        return args;
    }

    private Object[] buildBindArgsArray() {
        List<Object> args = buildBindArgs();
        return args.toArray(new Object[args.size()]);
    }

    private static class Projection {
        final String column;
        String expression;
        Object bindArg;

        Projection(String column, String expression) {
            this.column = column;
            this.expression = expression;
        }
    }
}
