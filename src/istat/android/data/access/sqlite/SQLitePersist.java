package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

public final class SQLitePersist {
    List<QueryAble> insertions = new ArrayList<QueryAble>();
    SQLite.SQL sql;

    SQLitePersist(SQLite.SQL sql) {
        this.sql = sql;
    }

    public SQLitePersist persist(Object insert) {
        try {
            QueryAble model = SQLiteModel.fromObject(insert);
            insertions.add(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLitePersist persist(List<?> insert) {
        for (Object obj : insert) {
            try {
                QueryAble model = SQLiteModel.fromObject(obj);
                insertions.add(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public SQLitePersist persist(Object... insert) {
        for (Object obj : insert) {
            try {
                QueryAble model = SQLiteModel.fromObject(obj);
                insertions.add(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public long[] execute() {
        if (insertions == null || insertions.size() == 0)
            return new long[]{0};
        long[] out = new long[insertions.size()];
        int index = 0;
        for (QueryAble insertion : insertions) {
            out[index] = insertion.persist(sql.db);
        }
        insertions.clear();
        notifyExecuted();
        return out;
    }

    private void notifyExecuted() {
        if (sql.autoClose) {
            sql.close();
        }
    }
}
