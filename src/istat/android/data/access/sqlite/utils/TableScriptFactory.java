package istat.android.data.access.sqlite.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import istat.android.data.access.sqlite.SQLite;
import istat.android.data.access.sqlite.SQLiteModel;

/**
 * Created by Istat Toukea on 21/11/2016.
 */

public class TableScriptFactory {
    Class<?> cLass;

    TableScriptFactory(Class<?> cLass) {
        this.cLass = cLass;
    }

    public static String drop(Class<?>... cLasss) throws IllegalAccessException, InstantiationException {
        String out = "";
        for (Class<?> cLass : cLasss) {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            out += "DROP TABLE " + model.getName() + ";";
        }
        return out;
    }

    public static String truncate(Class<?>... cLasss) throws IllegalAccessException, InstantiationException {
        String out = "";
        for (Class<?> cLass : cLasss) {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            out += "TRUNCATE TABLE " + model.getName() + ";";
        }
        return out;
    }

    public static String create(boolean findAll, Class<?>... cLasss) throws InstantiationException, IllegalAccessException {
        String out = "";
        for (Class<?> cLass : cLasss) {
            TableScriptFactory factory = new TableScriptFactory(cLass);
            out += "\n" + factory.create(findAll);
        }
        return out;
    }

    public static String create(Class<?>... cLasss) throws InstantiationException, IllegalAccessException {
        String out = "";
        for (Class<?> cLass : cLasss) {
            TableScriptFactory factory = new TableScriptFactory(cLass);
            out += "\n" + factory.create(false);
        }
        return out;
    }


    //----------------------------------------------
    public static String create(boolean findAll, HashMap<Class, LineAdapter> classAdapterPair, Class<?>... cLasss) throws InstantiationException, IllegalAccessException {

        String out = "";
        for (Class<?> cLass : cLasss) {
            TableScriptFactory factory = new TableScriptFactory(cLass);
            factory.adapterQueue.putAll(classAdapterPair);
            out += "\n" + factory.create(findAll);
        }
        return out;
    }

    public static String create(HashMap<Class, LineAdapter> classAdapterPair, Class<?>... cLasss) throws InstantiationException, IllegalAccessException {

        String out = "";
        for (Class<?> cLass : cLasss) {
            TableScriptFactory factory = new TableScriptFactory(cLass);
            factory.adapterQueue.putAll(classAdapterPair);
            out += "\n" + factory.create(false);
        }

        return out;
    }


    /*
         CREATE TABLE IF NOT EXISTS `commandes_tb` (
          `ID_CDE` INTEGER PRIMARY KEY AUTOINCREMENT,
          `NUM_CDE` varchar(50) NOT NULL,
          `MONTANT_COURSE` float NOT NULL,
          `AVANCE_A_PAYER` float NOT NULL,
          `ADRESSE_LIVRAISON` text,
          `ID_CLT_FK` bigint(20) NOT NULL,
          `CREATED_AT` datetime NOT NULL,
          `UPDATED_AT` datetime NOT NULL,
          `STATUS` char(3) NOT NULL DEFAULT '0',
          `JSON_CONTENT` text
        );
     */
    SQLiteModel model;

    public String create(boolean findAll) throws IllegalAccessException, InstantiationException {
        List<Field> fields;
        if (findAll) {
            fields = Toolkit.getAllFieldIncludingPrivateAndSuper(cLass);
        } else {
            fields = new ArrayList<Field>();
            Collections.addAll(fields, cLass.getDeclaredFields());
        }
        model = SQLiteModel.fromClass(cLass);
        String sql = "CREATE TABLE IF NOT EXISTS `" + model.getName() + "` (";
        int index = 0;
        for (Field field : fields) {
            if (field != null && field.toString().contains("static")) {
                continue;
            }
            String line = createLine(field);
            if (index > 0) {
                line = "," + line;
            }
            sql += line;
            index++;
        }
        sql += ");";
        return sql;
    }

    HashMap<Class, LineAdapter> adapterQueue = new HashMap() {
        {
            put(String.class, STRING_ADAPTER);
            put(Float.class, FLOAT_ADAPTER);
            put(Double.class, DOUBLE_ADAPTER);
            put(Integer.class, INTEGER_ADAPTER);
            put(Date.class, DATETIME_ADAPTER);

            put(float.class, FLOAT_ADAPTER);
            put(double.class, DOUBLE_ADAPTER);
            put(int.class, INTEGER_ADAPTER);
        }
    };

    static LineAdapter INTEGER_ADAPTER = new LineAdapter() {
        @Override
        String onCreateLine(Field field) {
            return "`" + field.getName() + "` INTEGER ";
        }
    };
    static LineAdapter FLOAT_ADAPTER = new LineAdapter() {
        @Override
        String onCreateLine(Field field) {
            return "`" + field.getName() + "` FLOAT ";
        }
    };
    static LineAdapter DOUBLE_ADAPTER = new LineAdapter() {
        @Override
        String onCreateLine(Field field) {
            return "`" + field.getName() + "` DOUBLE ";
        }
    };
    static LineAdapter STRING_ADAPTER = new LineAdapter() {
        @Override
        String onCreateLine(Field field) {
            return "`" + field.getName() + "` VARCHAR ";
        }
    };
    static LineAdapter DATETIME_ADAPTER = new LineAdapter() {
        @Override
        String onCreateLine(Field field) {
            return "`" + field.getName() + "` DATETIME ";
        }
    };

//    static LineAdapter BOOLEAN_ADAPTER = new LineAdapter() {
//        @Override
//        String onCreateLine(Field field) {
//            return "`" + field.getName() + "` TINYINT ";
//        }
//    };

    private String createLine(Field field) {
        Type type = field.getType();
        LineAdapter adapter = adapterQueue.get(type);
        if (adapter != null) {
            return adapter.createLine(field);
        } else {
            return adapterQueue.get(String.class).createLine(field);
        }
    }

    public static abstract class LineAdapter {
        abstract String onCreateLine(Field field);

        public String createLine(Field field) {
            return onCreateLine(field);
        }
    }
}
