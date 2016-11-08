package istat.android.data.access.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

public final class SQLiteInsert {
    List<QueryAble> insertions = new ArrayList<QueryAble>();
    SQLiteDatabase db;

    SQLiteInsert insert(Object insert, SQLiteDatabase db) {
        this.db = db;
        try {
            QueryAble model = SQLiteModel.fromObject(insert);
            insertions.add(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public SQLiteInsert insert(Object insert) {
        try {
            QueryAble model = SQLiteModel.fromObject(insert);
            insertions.add(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public long[] execute() {
        if (insertions == null || insertions.size() == 0)
            return new long[]{0};
        long[] out = new long[insertions.size()];
        int index = 0;
        for (QueryAble insertion : insertions) {
            out[index] = insertion.persist(db);
        }
        insertions.clear();
        return out;
    }
}