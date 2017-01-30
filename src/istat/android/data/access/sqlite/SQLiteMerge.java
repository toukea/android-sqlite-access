package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

public final class SQLiteMerge {
    List<SQLiteModel> merges = new ArrayList<SQLiteModel>();
    SQLite.SQL sql;

    SQLiteMerge(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLiteMerge merge(Object merge) {
        try {
            SQLiteModel model = SQLiteModel.fromObject(merge);
            merges.add(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteMerge merge(List<?> entities) {
        for (Object obj : entities) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj);
                merges.add(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLiteMerge merge(Object... merge) {
        for (Object obj : merge) {
            try {
                SQLiteModel model = SQLiteModel.fromObject(obj);
                merges.add(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute() {
        if (merges == null || merges.isEmpty())
            return new long[]{0};
        long[] out = new long[merges.size()];
        int index = 0;
        for (SQLiteModel merge : merges) {
            out[index] = merge.merge(sql.db);
        }
        merges.clear();
        notifyExecuted();
        return out;
    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }
}
