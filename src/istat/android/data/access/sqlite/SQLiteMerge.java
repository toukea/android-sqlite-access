package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

public final class SQLiteMerge {
    List<SQLiteModel> modelMerges = new ArrayList<SQLiteModel>();
    List<Object> merges = new ArrayList<Object>();
    SQLite.SQL sql;

    SQLiteMerge(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLiteMerge merge(Object merge) {
        try {
            SQLiteModel model = SQLiteModel.fromObject(merge,sql.serializer, sql.contentValueHandler);
            modelMerges.add(model);
            merges.add(merge);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteMerge merge(List<?> entities) {
        for (Object obj : entities) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,sql.serializer, sql.contentValueHandler);
                modelMerges.add(model);
                merges.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLiteMerge merge(Object... merge) {
        for (Object obj : merge) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj,sql.serializer, sql.contentValueHandler);
                modelMerges.add(model);
                merges.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute() {
        if (modelMerges == null || modelMerges.isEmpty())
            return new long[]{0};
        long[] out = new long[modelMerges.size()];
        int index = 0;
        for (SQLiteModel merge : modelMerges) {
            out[index] = merge.merge(sql.db);
            index++;
        }
        modelMerges.clear();
        notifyExecuted();
        return out;
    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }

    public List<Object> getMerges() {
        return merges;
    }
}
