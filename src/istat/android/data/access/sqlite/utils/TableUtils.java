package istat.android.data.access.sqlite.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import istat.android.data.access.sqlite.SQLiteModel;

/**
 * Created by istat on 24/01/17.
 */

public class TableUtils {
    public final static String[] fetchColumns(SQLiteDatabase db, String tableName) throws Exception {
        Cursor c = db.query(tableName, null, null, null, null, null, null);
        return c.getColumnNames();
    }

    @Deprecated
    public final static boolean exists(SQLiteDatabase db, Class cLass) {
        try {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            db.query(model.getName(), null, null, null, null, null, null);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public final static boolean isTableExists(SQLiteDatabase db, Class cLass) {
        try {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            db.query(model.getName(), null, null, null, null, null, null);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public final static boolean isTableExists(SQLiteDatabase db, String tableName) {
        try {
            db.query(tableName, new String[]{"count(1)"}, null, null, null, null, null);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public static void drop(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.drop(tables);
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public static void truncate(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.truncate(tables);
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public static void create(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.create(tables);
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public static void replace(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.drop(tables);
        scripts.addAll(TableScriptFactory.create(tables));
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public final static void init(SQLiteDatabase db, Class... tables) throws IllegalAccessException, InstantiationException {
        List<String> scripts = new ArrayList<String>();
        for (Class cLass : tables) {
            if (TableUtils.exists(db, cLass)) {
                scripts.addAll(TableScriptFactory.alter(cLass));
            } else {
                scripts.addAll(TableScriptFactory.create(cLass));
            }
        }
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public static boolean alter(SQLiteDatabase db, Class tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.alter(tables);
        return execute(db, scripts);
    }

    public static boolean execute(SQLiteDatabase db, String... scripts) {
        try {
            for (String sql : scripts) {
                db.execSQL(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean execute(SQLiteDatabase db, List<String> scripts) {
        for (String sql : scripts) {
            db.execSQL(sql);
        }
        return false;
    }
}
