package istat.android.data.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import istat.android.data.access.interfaces.JSONable;
import istat.android.data.access.utils.Toolkit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

abstract class SQLiteModel implements JSONable, QueryAble, Cloneable {
    HashMap<String, Object> map = new HashMap<String, Object>();
    private AnnotationParser parser;
    protected String tb_name, primary_key;
    protected String[] tb_projection;
    public static String TAG_CLASS = "istat.data.access.DbEntity.class";
    private Object instance;
    List<String> reflectionFieldNames = new ArrayList<String>();

    protected SQLiteModel() {
        instance = this;
        build();
    }

    protected void build() {
        tb_name = getEntityName();
        primary_key = getEntityPrimaryFieldName();
        tb_projection = getEntityFieldNames();
        parser = new AnnotationParser();
        try {
            parser.parse(this.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void set(String name, Object value) {
        set(name, this, value);
    }

    public String getPrimaryKey() {
        return getString(primary_key);
    }

    protected Object get(String name) {
        return get(name, this);// map.get(name);
    }

    protected String getString(String name) {
        Object value = get(name);
        return value == null ? "" : value.toString();
    }

    protected boolean getBoolean(String name) {
        return Boolean.valueOf(getString(name));
    }

    protected double getDouble(String name) {
        try {
            return Double.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    protected float getFloat(String name) {
        try {
            return Float.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    protected long getLong(String name) {
        try {
            return Long.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    protected int getInteger(String name) {
        try {
            return Integer.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    protected static <T extends SQLiteModel> void set(String name, T obj,
                                                      Object value) {
        try {
            if (obj.reflectionFieldNames.contains(name)) {
                Field field = obj.getClass().getDeclaredField(name);
                field.setAccessible(true);
                field.set(obj, value);
            } else {
                obj.map.put(name, value);
            }

        } catch (NoSuchFieldException noe) {
            obj.map.put(name, value);
            noe.printStackTrace();
        } catch (Exception e) {
            // Log.e("ERROR", name);
            e.printStackTrace();
            obj.map.put(name, value);

        }
    }

    protected static <T extends SQLiteModel> Object get(String name, T obj) {
        try {
            if (obj.reflectionFieldNames.contains(name)) {
                Field field = obj.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(obj);
            } else {
                return obj.map.get(name);
            }

        } catch (NoSuchFieldException noe) {
            noe.printStackTrace();
            return obj.map.get(name);

        } catch (Exception e) {
            // Log.e("ERROR", name);
            e.printStackTrace();
            return obj.map.get(name);

        }
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.putAll(map);
        try {
            parser.fillMap(getClass(), map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject json = createJsonFromHashMap(map);
        try {
            if (parser == null) {
                parser = new AnnotationParser();
            }
            parser.fillJSON(getClass(), json);
            String className = instance.getClass() + "";
            className = className.substring(6, className.length()).trim();
            json.put(TAG_CLASS, className);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }

    public JSONObject toJSONObject(boolean addclassName) {
        JSONObject json = toJSONObject();
        if (!addclassName) {
            json.remove(TAG_CLASS);
        }
        return json;
    }

    @Override
    public String toString() {
        return toJSONObject().toString();
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues pairs = new ContentValues();
        JSONObject toJson = toJSONObject();
        for (String projection : tb_projection) {
            String values = toJson.optString(projection);
            if (!TextUtils.isEmpty(values)) {
                pairs.put(projection, values);
            }
        }
        return pairs;
    }

    public final void fillFromJSONObject(JSONObject json) {

        onFillFromJson(json);

    }

    @Override
    public final void fillFromCursor(Cursor c) {

        onFillFromCursor(c);

    }

    public final void fillFromPrimaryKey(String primaryKey, SQLiteDatabase db) {
        onFillFromPrimaryKey(primaryKey, db);
    }

    public long merge(SQLiteDatabase db) {

        long out = 0;
        if (exist(db)) {
            out = update(db);
        } else {
            out = insert(db);
        }
        fillFromPrimaryKey(getPrimaryKey(), db);
        return out;
    }

    @Override
    public long persist(SQLiteDatabase db) {
        if (exist(db)) {
            return update(db);
        } else {
            return insert(db);
        }
    }

    protected void onPersistEmbeddedDbEntity(SQLiteDatabase db, QueryAble entity) {
        entity.persist(db);
    }

    protected void onFillFromPrimaryKey(String primaryKey, SQLiteDatabase db) {
        Cursor c = db.query(tb_name, tb_projection, primary_key + "=?",
                new String[]{primaryKey}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToNext();
            fillFromCursor(c);

        }
    }

    protected void onFillFromJson(JSONObject json) {
        try {
            List<String> keySet = JSONArrayToStringList(json.names());
            if (keySet.size() > 0) {
                for (String tmp : keySet) {
                    if (tmp.equals(TAG_CLASS))
                        continue;
                    Object value = createObject(tmp, json.optString(tmp));
                    if (value != null) {
                        set(tmp, value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onFillFromCursor(Cursor c) {
//        if (tb_projection.length <= 0) {
//            tb_projection = getEntityFieldNames();
//        }
        for (String projection : tb_projection) {
            int columnIndex = c.getColumnIndex(projection);
            if (columnIndex >= 0) {
                String values = c.getString(columnIndex);
                if (!TextUtils.isEmpty(values)) {
                    set(projection, values);
                }
            }
        }
    }

    public long insert(SQLiteDatabase db) {
        long out = 0;
        // db.beginTransaction();
        try {
            out = db.insert(getEntityName(), null, toContentValues());
            persistEmbeddedDbEntity(db);
            // db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // db.endTransaction();
        }

        return out;
    }

    public int update(SQLiteDatabase db) {

        int out = 0;
        // db.beginTransaction();
        try {
            out = update(db, primary_key + "= ?",
                    new String[]{getPrimaryKey()});
            persistEmbeddedDbEntity(db);
            // db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // db.endTransaction();
        }

        return out;
    }

    public void merge(SQLiteModel entity) {
        merge(entity, true, true);
    }

    public void merge(SQLiteModel entity, boolean override, boolean mergeEmptyValue) {
        HashMap<String, Object> bundle = entity.map;
        Iterator<String> keySet = bundle.keySet().iterator();
        while (keySet.hasNext()) {
            String tmp = keySet.next();
            Object obj = bundle.get(tmp);
            if (obj != null) {
                if (map.containsValue(obj) && override) {
                    if (!mergeEmptyValue && obj == null
                            || (TextUtils.isEmpty(obj.toString())))
                        ;
                    else
                        map.put(tmp, obj);

                } else {
                    map.put(tmp, obj);
                }

            }
        }
    }

    public boolean exist(SQLiteDatabase db) {
        if (TextUtils.isEmpty(primary_key)) {
            return false;
        }
        Cursor c = db.query(tb_name, new String[]{primary_key}, primary_key
                + "= ?", new String[]{getPrimaryKey()}, null, null, null);
        int count = c.getCount();
        c.close();
        return count > 0;
    }

    public int delete(SQLiteDatabase db) {

        return delete(db, primary_key + "= ?",
                new String[]{getPrimaryKey()});
    }

    public static <T extends SQLiteModel> T fromJson(String json)
            throws InstantiationException, IllegalAccessException,
            JSONException {
        return fromJson(new JSONObject(json), null);
    }

    public static <T extends SQLiteModel> T fromJson(String json, Class<T> clazz)
            throws InstantiationException, IllegalAccessException,
            JSONException {
        return fromJson(new JSONObject(json), clazz);
    }

    public static <T extends SQLiteModel> T fromQueryable(JSONable q)
            throws InstantiationException, IllegalAccessException {
        return fromJson(q.toJSONObject(), q.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T extends SQLiteModel> T fromJson(JSONObject json,
                                                     Class<?> cLass) throws InstantiationException,
            IllegalAccessException {
        if (json == null)
            return null;
        try {
            String clazz = json.optString(TAG_CLASS);
            if (!TextUtils.isEmpty(clazz)) {
                cLass = Class.forName(clazz);
            } else if (cLass == null) {
                return null;
            }
            try {
                Object obj = cLass.getConstructor(JSONObject.class)
                        .newInstance(json);
                if (obj instanceof SQLiteModel)
                    return (T) obj;
                else
                    return null;
            } catch (Exception e) {
                Object obj = Class.forName(clazz).newInstance();
                if (obj instanceof SQLiteModel) {
                    JSONable jsonentity = (SQLiteModel) obj;
                    jsonentity.fillFromJSONObject(json);
                    if (obj instanceof SQLiteModel)
                        return (T) jsonentity;
                    else
                        return null;
                } else {
                    return null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static SQLiteModel fromObject(final Object obj) throws InstantiationException,
            IllegalAccessException {
        String[] tmp = new String[0];
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(obj.getClass());
            tmp = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                tmp[i] = field.getName();
                map.put(tmp[i], field.get(obj));
            }

        } catch (Exception e) {
        }
        final String[] projections = tmp;
        SQLiteModel model = new SQLiteModel() {

            @Override
            public String getEntityName() {
                return obj.getClass().getSimpleName();
            }

            @Override
            public String[] getEntityFieldNames() {

                return projections;
            }

            @Override
            public String getEntityPrimaryFieldName() {
                String primaryKey = null;
                try {
                    List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(obj.getClass());
                    String[] fieldArray = new String[fields.size()];
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        if (field.isAnnotationPresent(PrimaryKey.class)) {
                            primaryKey = field.getName();
                        }
                    }
                } catch (Exception e) {

                }
                return primaryKey;
            }
        };
        model.map.putAll(map);
        return model;
    }

    public static SQLiteModel fromClass(final Class clazz) throws InstantiationException,
            IllegalAccessException {
        SQLiteModel model = new SQLiteModel() {
            @Override
            public String getEntityName() {
                return clazz.getSimpleName();
            }

            @Override
            public String[] getEntityFieldNames() {
                try {
                    List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(clazz);
                    String[] fieldArray = new String[fields.size()];
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        fieldArray[i] = field.getName();
                    }
                    return fieldArray;
                } catch (Exception e) {
                    return new String[0];
                }
            }

            @Override
            public String getEntityPrimaryFieldName() {
                String primaryKey = null;
                try {
                    List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(clazz);
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        if (field.isAnnotationPresent(PrimaryKey.class)) {
                            primaryKey = field.getName();
                        }
                    }
                } catch (Exception e) {

                }
                return primaryKey;
            }
        };
        return model;
    }

    private void persistEmbeddedDbEntity(SQLiteDatabase db) {
        try {
            Iterator<String> keySet = map.keySet().iterator();
            while (keySet.hasNext()) {
                String tmp = keySet.next();
                Object obj = map.get(tmp);
                if (obj != null && obj instanceof QueryAble) {
                    onPersistEmbeddedDbEntity(db, ((QueryAble) obj));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject createJsonFromHashMap(
            HashMap<String, Object> bundle) {
        try {
            JSONObject json = new JSONObject();
            Iterator<String> keySet = bundle.keySet().iterator();
            while (keySet.hasNext()) {
                String tmp = keySet.next();
                Object obj = bundle.get(tmp);
                if (obj != null) {
                    if (obj instanceof JSONable) {
                        JSONable jsonentity = (JSONable) obj;
                        json.put(tmp, jsonentity.toJSONObject());
                    } else {
                        String value = obj.toString();
                        if (!TextUtils.isEmpty(value)
                                && !value.equals(TAG_CLASS)) {
                            json.put(tmp, value);
                        }
                    }

                }
            }
            return json;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private Object createObject(String name, String objToString) {
        if (TextUtils.isEmpty(objToString))
            return null;
        try {
            JSONObject json = new JSONObject(objToString);
            String clazz = json.optString(TAG_CLASS);
            if (TextUtils.isEmpty(clazz)) {
                clazz = getFieldType(name);
            }
            if (TextUtils.isEmpty(clazz)) {
                return objToString;
            }
            try {
                Class<?> clazzs = Class.forName(clazz);
                Object obj = clazzs.getConstructor(JSONObject.class)
                        .newInstance(json);
                return obj;
            } catch (Exception e) {
                // e.printStackTrace();
                Object obj = Class.forName(clazz).newInstance();
                if (obj instanceof JSONable) {
                    JSONable jsonentity = (JSONable) obj;
                    jsonentity.fillFromJSONObject(json);
                    return jsonentity;
                } else {
                    return objToString;
                }
            }

        } catch (JSONException e) {

            return objToString;
        } catch (Exception e) {
            // e.printStackTrace();
            return objToString;
        }
    }

    private String getFieldType(String name) {
        try {
            if (this.reflectionFieldNames.contains(name)) {
                Field field = this.getClass().getDeclaredField(name);
                String className = field.getType() + "";
                className = className.substring(6, className.length()).trim();
                return className;
            }

        } catch (NoSuchFieldException noe) {

            noe.printStackTrace();
        } catch (Exception e) {
            // Log.e("ERROR", name);
            e.printStackTrace();

        }
        return null;
    }

    public <T> T asClass(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        T instance = clazz.newInstance();
        List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(clazz);
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType().isAssignableFrom(String.class)) {
                field.set(instance, getString(field.getName()));
            } else if (field.getType().isAssignableFrom(Double.class)) {
                field.set(instance, getDouble(field.getName()));
            } else if (field.getType().isAssignableFrom(Float.class)) {
                field.set(instance, getFloat(field.getName()));
            } else if (field.getType().isAssignableFrom(Long.class)) {
                field.set(instance, getLong(field.getName()));
            } else if (field.getType().isAssignableFrom(Boolean.class)) {
                field.set(instance, getBoolean(field.getName()));
            } else if (field.getType().isAssignableFrom(Integer.class)) {
                field.set(instance, getInteger(field.getName()));
            }
        }
        return instance;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Property {
        String info() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrimaryKey {

    }

    private boolean hasPrimaryKey() {

        return !TextUtils.isEmpty(getPrimaryKey());
    }

    protected void clear() {
        for (String name : tb_projection) {
            set(name, null);
        }
    }

    class AnnotationParser {
        public void parse(Class<?> clazz) throws Exception {
            List<String> projection = getEntityFieldNames() != null ? new ArrayList<String>(
                    Arrays.asList(getEntityFieldNames()))
                    : new ArrayList<String>();

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {

                if (field.isAnnotationPresent(PrimaryKey.class)
                        || field.isAnnotationPresent(Property.class)) {
                    field.setAccessible(true);
                    String name = field.getName();
                    reflectionFieldNames.add(name);
                    if (!projection.contains(name)) {
                        projection.add(name);
                    }
                }
                if (!hasPrimaryKey()
                        && field.isAnnotationPresent(PrimaryKey.class)) {
                    primary_key = field.getName();
                }
//                if (field.isAnnotationPresent(Projection.class)) {
//                    try {
//                        field.setAccessible(true);
//                        String[] tmp_prj = (String[]) field.get(instance);
//                        for (String item : tmp_prj) {
//                            if (!projection.contains(item)) {
//                                projection.add(item);
//                            }
//                        }
//                    } catch (Exception e) {
//                        // e.printStackTrace();
//                    }
//                }
//                if (field.isAnnotationPresent(TableName.class)) {
//                    field.setAccessible(true);
//                    tb_name = field.get(instance) + "";
//                }
            }
            tb_projection = new String[projection.size()];
            tb_projection = projection.toArray(tb_projection);

        }

        public void fillJSON(Class<?> clazz, JSONObject json)
                throws IllegalAccessException, IllegalArgumentException,
                JSONException {
            List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(clazz);
            for (Field field : fields) {
                if (field.isAnnotationPresent(PrimaryKey.class)
                        || field.isAnnotationPresent(Property.class)) {
                    field.setAccessible(true);
                    Object obj = field.get(instance);
                    if (obj instanceof JSONable) {
                        JSONable jsonEntity = (JSONable) obj;
                        json.put(field.getName(), jsonEntity.toJSONObject());
                    } else {
                        json.put(field.getName(), field.get(instance));
                    }
                } else {
                    // field.setAccessible(true);
                    // Log.d("filed::" + field.getName(), field.get(instance) +
                    // "");
                }
            }
        }

        public void fillMap(Class<?> clazz, HashMap<String, Object> map)
                throws IllegalAccessException, IllegalArgumentException,
                JSONException {
            List<Field> fields = Toolkit.getAllFieldIncludingPrivateAndSuper(clazz);
            for (Field field : fields) {
                if (field.isAnnotationPresent(PrimaryKey.class)
                        || field.isAnnotationPresent(Property.class)) {
                    field.setAccessible(true);
                    Object obj = field.get(instance);
                    if (obj != null && !TextUtils.isEmpty(obj + "")) {
                        map.put(field.getName(), obj);
                    }
                }
            }
        }
    }


    protected int update(SQLiteDatabase db, String whereClause,
                         String[] whereArgs) {
        return db.update(getEntityName(), toContentValues(), whereClause,
                whereArgs);
    }

    protected int delete(SQLiteDatabase db, String whereClause,
                         String[] whereArgs) {
        return db.delete(getEntityName(), whereClause, whereArgs);
    }

    public static ContentValues createContentValuesFromJSONObject(
            JSONObject json) throws JSONException {
        ContentValues pair = new ContentValues();
        List<String> keySet = JSONArrayToStringList(json.names());
        if (keySet.size() > 0) {
            for (String tmp : keySet) {
                String value = json.optString(tmp);
                if (!TextUtils.isEmpty(value)) {
                    pair.put(tmp, value);
                }
            }
        }

        return pair;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static List<String> JSONArrayToStringList(JSONArray array)
            throws JSONException {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++)
            out.add(array.getString(i));
        return out;
    }

}
