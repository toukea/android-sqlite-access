package istat.android.data.access;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

public class SQLiteInsert {
    List<QueryAble> insertions = new ArrayList<QueryAble>();
    SQLiteDatabase db;

    SQLiteInsert insert(Object insert, SQLiteDatabase db) {
        this.db = db;
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
