package istat.android.data.access.sqlite.utils;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.sqlite.SQLiteModel;

/**
 * Created by istat on 07/02/17.
 */

class SQLiteAlterTable {
    static String addColumn(String table) {
        return "ALTER TABLE " + table + " ADD column_name datatype";
    }

    public final static List<String> createAlterScripts(Class current, Class target) throws IllegalAccessException, InstantiationException {
        List<String> list = new ArrayList<String>();
        SQLiteModel model1=SQLiteModel.fromClass(current);
        SQLiteModel model2=SQLiteModel.fromClass(target);
        return list;
    }
}
