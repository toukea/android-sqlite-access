package istat.android.data.access.sqlite.utils;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import istat.android.data.access.sqlite.SQLiteModel;

/**
 * Created by Istat Toukea on 21/11/2016.
 */

public class TableScriptFactory {
    Class<?> cLass;

    TableScriptFactory(Class<?> cLass) {
        this.cLass = cLass;
    }

    public static List<String> drop(Class<?>... cLasss) throws IllegalAccessException, InstantiationException {
        List<String> out = new ArrayList<String>();
        for (Class<?> cLass : cLasss) {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            out.add("DROP TABLE " + model.getName() + ";");
        }
        return out;
    }

    public static List<String> truncate(Class<?>... cLasss) throws IllegalAccessException, InstantiationException {
        List<String> out = new ArrayList<String>();
        for (Class<?> cLass : cLasss) {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            out.add("TRUNCATE TABLE " + model.getName() + ";");
        }
        return out;
    }

    public static List<String> create(Class<?>... cLasss) throws InstantiationException, IllegalAccessException {
        return create(null, cLasss);
    }


    public static List<String> create(HashMap<Class, FieldAdapter> classAdapterPair, Class<?>... cLasss) throws InstantiationException, IllegalAccessException {

        List<String> out = new ArrayList<String>();
        for (Class<?> cLass : cLasss) {
            TableScriptFactory factory = new TableScriptFactory(cLass);
            if (classAdapterPair != null && !classAdapterPair.isEmpty()) {
                factory.adapterQueue.putAll(classAdapterPair);
            }
            Class<?>[] nestedClasses = cLass.getClasses();
            if (nestedClasses != null && nestedClasses.length > 0) {
                for (Class<?> nestedClass : nestedClasses) {
                    if (nestedClass.isAnnotationPresent(SQLiteModel.Table.class)) {
                        out.addAll(create(classAdapterPair, nestedClass));
                    }
                }
            }
            out.add(factory.create());
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

    public String create() throws IllegalAccessException, InstantiationException {
        SQLiteModel model = SQLiteModel.fromClass(cLass);
        String sql = "CREATE TABLE IF NOT EXISTS `" + model.getName() + "` (";
        int index = 0;
        for (String columnName : model.getProjections()) {
            Field field = model.getField(columnName);
            if (field != null) {
                String line = createLine(model, columnName, field);
                if (!TextUtils.isEmpty(line)) {
                    if (index > 0) {
                        line = "," + line;
                    }
                    sql += line;
                    index++;
                }
            }
        }
        sql += ");";
        return sql;
    }

    HashMap<Class, FieldAdapter> adapterQueue = new HashMap() {
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

    static FieldAdapter INTEGER_ADAPTER = new FieldAdapter() {
        @Override
        String onCreateLine(String columnName, Field field) {
            return "`" + columnName + "` INTEGER ";
        }
    };
    static FieldAdapter FLOAT_ADAPTER = new FieldAdapter() {
        @Override
        String onCreateLine(String columnName, Field field) {
            return "`" + columnName + "` FLOAT ";
        }
    };
    static FieldAdapter DOUBLE_ADAPTER = new FieldAdapter() {
        @Override
        String onCreateLine(String columnName, Field field) {
            return "`" + columnName + "` DOUBLE ";
        }
    };
    static FieldAdapter STRING_ADAPTER = new FieldAdapter() {
        @Override
        String onCreateLine(String columnName, Field field) {
            return "`" + columnName + "` VARCHAR ";
        }
    };
    static FieldAdapter DATETIME_ADAPTER = new FieldAdapter() {
        @Override
        String onCreateLine(String columnName, Field field) {
            return "`" + columnName + "` DATETIME ";
        }
    };

    private String createLine(SQLiteModel model, String columnName, Field field) {
        String out;
        Type type = field.getType();
        FieldAdapter adapter = adapterQueue.get(type);
        if (adapter != null) {
            out = adapter.createLine(columnName, field);
        } else {
            out = adapterQueue.get(String.class).createLine(columnName, field);
        }
        if (columnName.equals(model.getPrimaryFieldName())) {
            int policy = model.getPrimaryKeyPolicy();
            out += " PRIMARY KEY ";
        }
        return out;
    }

    public static abstract class FieldAdapter {
        abstract String onCreateLine(String columnName, Field field);

        public final String createLine(String columnName, Field field) {
            return onCreateLine(columnName, field);
        }
    }
}
