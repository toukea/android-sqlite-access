package istat.android.data.access;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import istat.android.data.access.interfaces.Queryable;

public class DbInsert {
	List<Queryable> insertions = new ArrayList<Queryable>();

	DbInsert insert(Queryable insert) {
		insertions.add(insert);
		return this;
	}

	public long[] execute(SQLiteDatabase db) {
		if (insertions == null || insertions.size() == 0)
			return new long[] { 0 };
		long[] out = new long[insertions.size()];
		int index = 0;
		for (Queryable insertion : insertions) {
			out[index] = insertion.persist(db);
		}
		insertions.clear();
		return out;
	}
}
