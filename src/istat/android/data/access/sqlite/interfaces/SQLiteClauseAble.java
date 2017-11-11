package istat.android.data.access.sqlite.interfaces;

import istat.android.data.access.sqlite.SQLite;

/**
 * Created by istat on 27/07/17.
 */

public interface SQLiteClauseAble {
    SQLite.SQL getInternalSQL();
}
