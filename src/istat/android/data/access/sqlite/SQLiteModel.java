package istat.android.data.access.sqlite;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import istat.android.data.access.sqlite.interfaces.JSONable;
import istat.android.data.access.sqlite.interfaces.QueryAble;
import istat.android.data.access.sqlite.utils.Toolkit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

public abstract class SQLiteModel implements JSONable, QueryAble, Cloneable, Iterable {

    Class<?> modelClass = Object.class;
    HashMap<String, Object> fieldNameValuePair = new HashMap();
    HashMap<String, Field> columnNameFieldPair = new HashMap();
    HashMap<String, Field> nestedTableFieldPair = new HashMap();
    private final static HashMap<Class, Builder> BUILDER_BUFFER = new HashMap();
    public static final String TAG_CLASS = SQLiteModel.class.getCanonicalName() + ".CLASS";
    public static final String TAG_ITEMS = SQLiteModel.class.getCanonicalName() + ".ITEMS";
    public final static String DEFAULT_PRIMARY_KEY_NAME = "id";

    SQLiteModel() {

    }

    public final void set(String name, Object value) {
        this.fieldNameValuePair.put(name, value);
    }

    public final void setPrimaryKeyValue(Object value) {
        this.fieldNameValuePair.put(getPrimaryKeyName(), value);
    }

    public final Object get(String name) {
        return this.fieldNameValuePair.get(name);
    }

    public final boolean hasColumn(String name) {
        return this.fieldNameValuePair.containsKey(name);
    }

    public final boolean isPrimaryKeyValueSet() {
        if (!hasColumn(getPrimaryKeyName())) {
            return false;
        }
        Object primaryKeyValue = getPrimaryKeyValue();
        return primaryKeyValue != null && !TextUtils.isEmpty(primaryKeyValue.toString());
    }

    public final boolean isEmpty(String name) {
        return TextUtils.isEmpty(getString(name));
    }

    public final boolean isNULL(String name) {
        return get(name) == null;
    }

    public final String getString(String name) {
        Object value = get(name);
        return value == null ? null : value.toString();
    }

    public final boolean getBoolean(String name) {
        return Boolean.valueOf(getString(name));
    }

    public final double getDouble(String name) {
        try {
            return Double.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    public final float getFloat(String name) {
        try {
            String value = getString(name);
            return Float.valueOf(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public final long getLong(String name) {
        try {
            return Long.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    public final int getInteger(String name) {
        try {
            return Integer.valueOf(getString(name));
        } catch (Exception e) {
            return 0;
        }
    }

    public String getPrimaryKeyStringValue() {
        return getString(getPrimaryKeyName());
    }

    public Object getPrimaryKeyValue() {
        return get(getPrimaryKeyName());
    }

    public Class<?> getModelClass() {
        return modelClass;
    }


    public String getSerializedValue(String name) {
        Object value = get(name);
        if (value == null) {
            return null;
        }
        return serializer.onSerialize(value, name);
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.putAll(this.fieldNameValuePair);
        return map;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = createJsonFromHashMap(this.serializer, fieldNameValuePair);
        try {
            String className = modelClass.getCanonicalName();
            json.put(TAG_CLASS, className);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }

    public JSONObject toJson(boolean addClassName) {
        JSONObject json = toJson();
        if (!addClassName) {
            json.remove(TAG_CLASS);
        }
        return json;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public String toString(int indentSpaces) {
        try {
            return toJson().toString(indentSpaces);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return toString();
    }


    @Override
    public ContentValues toContentValues() {
        ContentValues contentValues = contentValueHandler.toContentValues(this);
        //TODO specifier le contentValue afin que les foreignKey sont ajouté a la place du Gson de l'object e lui même.
        return contentValues;
    }

    public boolean isPrimaryFieldDefined() {
        return !TextUtils.isEmpty(getPrimaryKeyName());
    }

    public final void fillFromJson(JSONObject json) {
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

    @Override
    public final void fillFromCursor(Cursor c) {
        fillFromCursor(c, DEFAULT_CURSOR_READER);
    }

    public final void fillFromCursor(Cursor cursor, CursorReader reader) {
        if (reader == null) {
            reader = DEFAULT_CURSOR_READER;
        }
        if (cursor != null) {
            reader.onReadCursor(this, cursor);
        }
    }

    public final void fillFromPrimaryKey(String primaryKey, SQLiteDatabase db) {
        String tb_name = getName();
        String[] tb_projection = getColumns();
        String primary_key_name = getPrimaryKeyName();
        Cursor c = db.query(tb_name, tb_projection, primary_key_name + "=?",
                new String[]{primaryKey}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToNext();
            fillFromCursor(c);
        }
        c.close();
    }

    public void refresh(SQLiteDatabase db) {
        fillFromPrimaryKey(getPrimaryKeyStringValue(), db);
    }

    public long merge(SQLiteDatabase db) {
        if (exist(db)) {
            long out = update(db);
            refresh(db);
            return out >= 1 ? 0 : -1;
        } else {
            return insert(db, true);
        }
    }

    public long persist(SQLiteDatabase db) {
        if (exist(db)) {
            long out = update(db);
            return out >= 1 ? 0 : -1;
        } else {
            return insert(db, true);
        }
    }

    protected void onPersistEmbeddedDbEntity(SQLiteDatabase db, Class cLass, SQLiteModel embeddedModel) throws SQLiteException {
        embeddedModel.persist(db);
    }

    @Override
    public long insert(SQLiteDatabase db) throws SQLiteException {
        return insert(db, false);
    }

    private long insert(SQLiteDatabase db, boolean updateModelPrimaryKey) throws SQLiteException {
        long insertId;
        String tbName = getName();
        ContentValues contentValues = toContentValues();
        insertId = db.insert(tbName, null, contentValues);
        if (updateModelPrimaryKey && !isPrimaryKeyValueSet()) {
//            Object primaryKeyValue = insertId;
//            if (!TextUtils.isEmpty(getPrimaryKeyStringValue())) {
//                if (contentValues.containsKey(getPrimaryKeyName())) {
//                    primaryKeyValue = contentValues.get(getPrimaryKeyName());
//                }
//            } /*else {
//                primaryKeyValue = getPrimaryKeyValue();
//                if (Number.class.isAssignableFrom(primaryKeyValue.getClass())) {
//                    primaryKeyValue = insertId;
//                }
//            }*/
//            setPrimaryKeyValue(primaryKeyValue);
            setPrimaryKeyValue(insertId);
        }
        persistNestedEntity(db);
        return insertId;
    }

    public int update(SQLiteDatabase db) throws SQLiteException {
        int out;
        try {
            out = update(db, getPrimaryKeyName() + "= ?", new String[]{getPrimaryKeyStringValue()});
            persistNestedEntity(db);
        } catch (Exception e) {
            e.printStackTrace();
            SQLiteException error = new SQLiteException("Error durring update.");
            error.initCause(e);
            throw error;
        } finally {
        }
        return out;
    }

    public void merge(SQLiteModel entity) {
        merge(entity, true, true);
    }

    public void merge(SQLiteModel entity, boolean override, boolean mergeEmptyValue) {
        HashMap<String, Object> bundle = entity.fieldNameValuePair;
        Iterator<String> keySet = bundle.keySet().iterator();
        while (keySet.hasNext()) {
            String tmp = keySet.next();
            Object obj = bundle.get(tmp);
            if (obj != null) {
                if (fieldNameValuePair.containsValue(obj) && override) {
                    if (!mergeEmptyValue && obj == null
                            || (TextUtils.isEmpty(obj.toString()))) {

                    } else {
                        fieldNameValuePair.put(tmp, obj);
                    }
                } else {
                    fieldNameValuePair.put(tmp, obj);
                }

            }
        }
    }

    public boolean exist(SQLiteDatabase db) {
        String primary_key_name = getPrimaryKeyName();
        String primaryKeyValue = getPrimaryKeyStringValue();
        String tb_name = getName();
        if (TextUtils.isEmpty(primary_key_name) || TextUtils.isEmpty(primaryKeyValue)) {
            return false;
        }
        try {

            Cursor c = db.query(tb_name, new String[]{primary_key_name}, primary_key_name
                    + "= ?", new String[]{primaryKeyValue}, null, null, null);
            int count = c.getCount();
            c.close();
            return count > 0;
        } catch (Exception e) {
            new RuntimeException(e.getMessage() + ": Table=" + tb_name, e);
        }
        return false;
    }

    public int delete(SQLiteDatabase db) {
        return delete(db, getPrimaryKeyName() + "= ?",
                new String[]{getPrimaryKeyStringValue()});
    }

    public static SQLiteModel fromJson(final String tableName, final String primaryKeyName, final String[] columns, JSONObject json) {
        SQLiteModel model = new SQLiteModel() {
            @Override
            public String getName() {
                return tableName;
            }

            @Override
            public String[] getColumns() {
                return columns;
            }

            @Override
            public String getPrimaryKeyName() {
                return primaryKeyName;
            }
        };
        model.fillFromJson(json);
        return model;
    }

    public static SQLiteModel fromObject(final Object obj) throws InstantiationException,
            IllegalAccessException {
        return fromObject(obj, SQLiteModel.DEFAULT_SERIALIZER, DEFAULT_CONTAIN_VALUE_HANDLER);
    }

    @SuppressWarnings("unchecked")
    public static SQLiteModel fromObject(final Object obj, Serializer serializer, ContentValueHandler contentValueHandler) throws InstantiationException,
            IllegalAccessException {
        try {
            if (obj instanceof SQLiteModel) {
                SQLiteModel model = (SQLiteModel) obj;
                return (SQLiteModel) model.clone();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Builder builder = new Builder();
        Class<?> cLass = obj.getClass();
        List<String> tmp = new ArrayList<String>();
        HashMap<String, Object> fieldNameValuePair = new HashMap<String, Object>();
        HashMap<String, Field> columnNameFieldPair = new HashMap<String, Field>();
        HashMap<String, Field> nestedTableField = new HashMap<String, Field>();
        boolean hasColumnAnnotation = false;
        String primaryKey = null;
        String eligiblePrimaryName = null;
        String tableName = null;
        if (tableName == null) {
            tableName = cLass.getSimpleName();
        }
        if (TextUtils.isEmpty(tableName)) {
            cLass = (Class<?>) cLass.getGenericSuperclass();
            tableName = cLass.getSimpleName();
        }
        try {

            List<Field> fields = Toolkit.getAllFieldFields(cLass, true, false);
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                field.setAccessible(true);
                if (!field.isAnnotationPresent(Ignore.class)) {
                    String columnName = null;
                    if (field.isAnnotationPresent(PrimaryKey.class) && primaryKey == null) {
                        primaryKey = field.getName();
                    } else if (field.getName().equalsIgnoreCase(DEFAULT_PRIMARY_KEY_NAME)) {
                        eligiblePrimaryName = field.getName();
                    }
                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        columnName = column.name();
                        if (!hasColumnAnnotation) {
                            tmp.clear();
                        }
                        hasColumnAnnotation = true;
                    }
                    if (columnName == null && !hasColumnAnnotation) {
                        columnName = field.getName();
                    }
                    if (columnName != null && !tmp.contains(columnName)) {
                        tmp.add(columnName);
                        Object value = field.get(obj);
                        fieldNameValuePair.put(columnName, value);
                        columnNameFieldPair.put(columnName, field);

                        if (isNestedTableProperty(field)) {
                            //TODO implement more advanced processing with nested table
                            nestedTableField.put(columnName, field);
                        }
                    }
                }
            }
            if (isCollection(cLass)) {
                Collection<?> modelAsCollection = (Collection<?>) obj;
                if (!modelAsCollection.isEmpty()) {
                    List tmpList = new ArrayList();
                    tmpList.addAll(modelAsCollection);
                    fieldNameValuePair.put(TAG_ITEMS, tmpList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (primaryKey == null) {
            primaryKey = eligiblePrimaryName;
        }
        if (!tmp.contains(primaryKey) && primaryKey != null) {
            tmp.add(primaryKey);
        }
        final String[] projections = tmp.toArray(new String[tmp.size()]);
        final String primary = primaryKey;
        //---------------------------------

        if (cLass.isAnnotationPresent(Table.class)) {
            Table table = cLass.getAnnotation(Table.class);
            tableName = table.name();
        }
        builder.setName(tableName)
                .setColumns(projections)
                .setPrimaryFieldName(primary)
                .setColumnNameFieldPair(columnNameFieldPair)
                .setNestedTableNameFieldPair(nestedTableField)
                .setModelClass(cLass)
                .setSerializer(serializer)
                .setContentValueHandler(contentValueHandler);
        return builder.create(fieldNameValuePair);
    }

//    public final static <T, Y> SQLiteModel fromMap(String tableName, Map<T, Y> map) {
//
//    }

    public static <T> List<T> buildAsArrays(Class<T> cLass, Cursor c, CursorReader reader) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        List<T> list = new ArrayList<T>();
        while (c.moveToNext()) {
            list.add(buildAs(cLass, c, reader));
        }
        return list;
    }

    public static <T> T buildAs(Class<T> cLass, Cursor c, CursorReader reader) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        SQLiteModel model = SQLiteModel.fromClass(cLass);
        model.fillFromCursor(c, reader);
        return model.asInstance(cLass);
    }

    public static SQLiteModel fromClass(final Class cLass) throws InstantiationException,
            IllegalAccessException {
        return fromClass(cLass, SQLiteModel.DEFAULT_SERIALIZER, DEFAULT_CONTAIN_VALUE_HANDLER);
    }

    public static SQLiteModel fromClass(Class cLass, Serializer serializer, ContentValueHandler contentValueHandler) throws InstantiationException,
            IllegalAccessException {
        if (serializer == null) {
            serializer = DEFAULT_SERIALIZER;
        }
        if (contentValueHandler == null) {
            contentValueHandler = DEFAULT_CONTAIN_VALUE_HANDLER;
        }
        Builder builder = BUILDER_BUFFER.get(cLass);
        if (builder == null) {
            builder = new Builder();
            List<String> projectionAdder = new ArrayList<String>();
            HashMap<String, Field> nameFieldPair = new HashMap<String, Field>();
            HashMap<String, Field> nestedTableField = new HashMap<String, Field>();
            boolean hasColumnAnnotation = false;
            String primaryKey = null;
            String eligiblePrimaryName = null;
            String tableName = null;
            if (tableName == null) {
                tableName = cLass.getSimpleName();
            }
            if (TextUtils.isEmpty(tableName)) {
                cLass = (Class<?>) cLass.getGenericSuperclass();
                tableName = cLass.getSimpleName();
            }
            try {
                List<Field> fields = Toolkit.getAllFieldFields(cLass, true, false);
                for (int i = 0; i < fields.size(); i++) {
                    Field field = fields.get(i);
                    if (!field.isAnnotationPresent(Ignore.class)) {
                        String columnName = null;
                        if (field.isAnnotationPresent(PrimaryKey.class) && primaryKey == null) {
                            primaryKey = field.getName();
                        } else if (field.getName().equalsIgnoreCase(DEFAULT_PRIMARY_KEY_NAME)) {
                            eligiblePrimaryName = field.getName();
                        }
                        if (field.isAnnotationPresent(Column.class)) {
                            Annotation columnAnnotation = field.getAnnotation(Column.class);
                            Column column = (Column) columnAnnotation;
                            columnName = column.name();
                            if (!hasColumnAnnotation) {
                                projectionAdder.clear();
                            }
                            hasColumnAnnotation = true;
                        }
                        if (columnName == null && !hasColumnAnnotation) {
                            columnName = field.getName();
                        }
                        if (columnName != null && !projectionAdder.contains(columnName)) {
                            projectionAdder.add(columnName);
                            nameFieldPair.put(columnName, field);
                            if (isNestedTableProperty(field)) {
                                nestedTableField.put(columnName, field);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (primaryKey == null) {
                primaryKey = eligiblePrimaryName;
            }
            if (!projectionAdder.contains(primaryKey) && primaryKey != null) {
                projectionAdder.add(primaryKey);
            }
            final String[] projections = projectionAdder.toArray(new String[projectionAdder.size()]);
            final String primary = primaryKey;
            //--------------------------------------------------
            if (cLass.isAnnotationPresent(Table.class)) {
                Table table = (Table) cLass.getAnnotation(Table.class);
                tableName = table.name();
            }

            builder.setName(tableName)
                    .setColumns(projections)
                    .setPrimaryFieldName(primary)
                    .setColumnNameFieldPair(nameFieldPair)
                    .setNestedTableNameFieldPair(nestedTableField)
                    .setModelClass(cLass);
            BUILDER_BUFFER.put(cLass, builder);
        }

        builder.setSerializer(serializer)
                .setContentValueHandler(contentValueHandler);
        return builder.create();
    }


    private void persistNestedEntity(SQLiteDatabase db) {
        try {
            Iterator<String> keySet = nestedTableFieldPair.keySet().iterator();
            while (keySet.hasNext()) {
                String tableName = keySet.next();
                Field field = nestedTableFieldPair.get(tableName);
                if (field != null) {
                    traitNestedTableField(field, db);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SQLiteException error = new SQLiteException("Error durring embedded persistance.");
            error.initCause(e);
            throw error;
        }
    }

    //TODO query for Annotation embedded persistence mod
    private void traitNestedTableField(Field field, SQLiteDatabase db) throws IllegalAccessException, InstantiationException {
        Class<?> cLass = Toolkit.getFieldTypeClass(field);
        SQLiteModel model = SQLiteModel.fromObject(cLass);
        onPersistEmbeddedDbEntity(db, cLass, model);
    }

    private static JSONObject createJsonFromHashMap(Serializer serializer,
                                                    HashMap<String, Object> bundle) {
        return createJsonFromHashMap(serializer, bundle, false);
    }

    private static JSONObject createJsonFromHashMap(Serializer serializer,
                                                    HashMap<String, Object> bundle, boolean acceptEmpty) {
        try {
            JSONObject json = new JSONObject();
            Iterator<String> keySet = bundle.keySet().iterator();
            while (keySet.hasNext()) {
                String name = keySet.next();
                Object obj = bundle.get(name);
                if (obj == null && !acceptEmpty) {
                    if (acceptEmpty) {
                        json.put(name, null);
                    }
                    continue;
                }
                if (obj != null) {
                    if (obj instanceof JSONable) {
                        JSONable jsonModel = (JSONable) obj;
                        json.put(name, jsonModel.toJson());
                    } else {
                        String value = serializer.onSerialize(obj, name);
                        if (!TextUtils.isEmpty(value)
                                && !value.equals(TAG_CLASS)) {
                            json.put(name, value);
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
                clazz = getFieldTypeClass(name).getCanonicalName();
            }
            if (TextUtils.isEmpty(clazz)) {
                return objToString;
            }
            try {
                Class<?> cLass = Class.forName(clazz);
                Object obj = cLass.getConstructor(JSONObject.class)
                        .newInstance(json);
                return obj;
            } catch (Exception e) {
                e.printStackTrace();
                Object obj = Class.forName(clazz).newInstance();
                if (obj instanceof JSONable) {
                    JSONable jsonModel = (JSONable) obj;
                    jsonModel.fillFromJson(json);
                    return jsonModel;
                } else {
                    return objToString;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return objToString;
        } catch (Exception e) {
            e.printStackTrace();
            return objToString;
        }
    }

    public Field getField(String columnName) {
        if (columnNameFieldPair.containsKey(columnName)) {
            return columnNameFieldPair.get(columnName);
        } else {
            return null;
        }
    }

    public Class<?> getFieldTypeClass(String fieldName) {
        if (fieldNameValuePair.containsKey(fieldName)) {
            Object obj = get(fieldName);
            if (obj != null) {
                return obj.getClass();
            }
        } else {
            Field field = getField(fieldName);
            if (field != null) {
                return field.getType();
            }
        }
        return Object.class;
    }

    public <T> T asInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return asInstance(clazz, this.serializer);
    }

    //TODO update to combine serializer and cursorReader.
    public <T> T asInstance(Class<T> clazz, Serializer serializer) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        T instance = Toolkit.newInstance(clazz);
        List<Field> fields = Toolkit.getAllFieldFields(clazz, true, false);
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Ignore.class)) {
                try {
                    field.setAccessible(true);
                    Object value = get(field.getName());
                    if (value == null) {
                        continue;
                    }
                    Object obj;
                    if (isNestedTableProperty(field)) {
                        //TODO a la plca d1e total decerialisation effectuer une nouvelle requete pour determiner l'object a partir de la ou des foreignKey
                        obj = serializer.onDeSerialize(String.valueOf(value), field);
                    } else {
                        obj = serializer.onDeSerialize(String.valueOf(value), field);
                    }
                    if (field != null) {
                        field.set(instance, obj);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return instance;
    }

    private static boolean isNestedTableProperty(Field field) {
        return field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToMany.class)
                || field.isAnnotationPresent(ManyToOne.class);
    }

    public static String getFieldColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Annotation columnAnnotation = field.getAnnotation(Column.class);
            Column column = (Column) columnAnnotation;
            return column.name();
        }
        return null;
    }

    public static String getFieldNestedMappingName(Field field) {
        String mappedBy = null;
        if (field.isAnnotationPresent(OneToOne.class)) {
            Annotation columnAnnotation = field.getAnnotation(OneToOne.class);
            OneToOne column = (OneToOne) columnAnnotation;
            mappedBy = column.mappedBy();
        } else if (field.isAnnotationPresent(OneToMany.class)) {
            Annotation columnAnnotation = field.getAnnotation(OneToMany.class);
            OneToMany column = (OneToMany) columnAnnotation;
            mappedBy = column.mappedBy();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            Annotation columnAnnotation = field.getAnnotation(ManyToMany.class);
            ManyToMany column = (ManyToMany) columnAnnotation;
            mappedBy = column.mappedBy();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            Annotation columnAnnotation = field.getAnnotation(ManyToOne.class);
            ManyToOne column = (ManyToOne) columnAnnotation;
            mappedBy = column.mappedBy();
        }
        if (TextUtils.isEmpty(mappedBy)) {
            mappedBy = field.getType().getSimpleName() + "_id";
        }
        return mappedBy;
    }

    public void fillFromContentValues(ContentValues contentValue) {
        Iterator<Map.Entry<String, Object>> iterator = contentValue.valueSet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> name = iterator.next();
            set(name.getKey(), name.getValue());
        }
    }

    //TODO make it better
    public static <T> T cursorAsClass(Cursor c, Class<T> clazz, Serializer serializer, CursorReader cursorReader) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        SQLiteModel model = SQLiteModel.fromClass(clazz,
                serializer, null);
        model.fillFromCursor(c, cursorReader);
        T obj = model.asInstance(clazz);
        return obj;
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Table {
        String name() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Column {
        String name() default "";

        boolean nullable() default true;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrimaryKey {
        int POLICY_AUTO_GENERATE = 2;
        int POLICY_AUTO_INCREMENT = 1;
        int POLICY_NONE = 0;
        int POLICY_DEFAULT = -1;

        int policy() default POLICY_DEFAULT;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ignore {
        int when() default 0;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotNull {
    }


    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OneToOne {
        String mappedBy();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OneToMany {
        String mappedBy();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManyToOne {
        String mappedBy();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManyToMany {
        String mappedBy() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Link {
        String type();

        String mappedBy() default "";
    }


    private boolean hasPrimaryKey() {
        return !TextUtils.isEmpty(getPrimaryKeyStringValue());
    }

    protected void clear() {
        String[] projections = getColumns();
        for (String name : projections) {
            set(name, null);
        }
    }


    protected int update(SQLiteDatabase db, String whereClause,
                         String[] whereArgs) {
        return db.update(getName(), toContentValues(), whereClause,
                whereArgs);
    }

    protected int delete(SQLiteDatabase db, String whereClause,
                         String[] whereArgs) {
        return db.delete(getName(), whereClause, whereArgs);
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

    public int getPrimaryKeyPolicy() {
        Field field = getField(getPrimaryKeyName());
        if (field != null && field.isAnnotationPresent(PrimaryKey.class)) {
            PrimaryKey primary = field.getAnnotation(PrimaryKey.class);
            return primary.policy();
        }
        return 0;
    }

    String[] getNestedTableColumnNames() {
        String[] out = new String[nestedTableFieldPair.size()];
        Iterator<String> iterator = nestedTableFieldPair.keySet().iterator();
        int index = 0;
        while (iterator.hasNext()) {
            out[index] = iterator.next();
            index++;
        }
        return out;
    }

    Field[] getNestedTableFields() {
        Field[] out = new Field[nestedTableFieldPair.size()];
        Iterator<String> iterator = nestedTableFieldPair.keySet().iterator();
        int index = 0;
        while (iterator.hasNext()) {
            String name = iterator.next();
            out[index] = nestedTableFieldPair.get(name);
            index++;
        }
        return out;
    }

    public final static class Builder {
        Serializer serializer = SQLiteModel.DEFAULT_SERIALIZER;
        ContentValueHandler contentValueHandler = DEFAULT_CONTAIN_VALUE_HANDLER;
        Class<?> modelClass = Object.class;
        String name, primaryFieldName;
        List<String> projections = new ArrayList<String>();
        // HashMap<String, Object> FieldNameValuePair = new HashMap();
        HashMap<String, Field> columnNameFieldPair = new HashMap();
        HashMap<String, Field> nestedTableNameFieldPair = new HashMap();

        public Builder setSerializer(Serializer serializer) {
            if (serializer == null) {
                serializer = DEFAULT_SERIALIZER;
            }
            this.serializer = serializer;
            return this;
        }

        public Builder setPrimaryFieldName(String name) {
            this.primaryFieldName = name;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addColumn(String... name) {
            for (String n : name) {
                this.projections.add(n);
            }
            return this;
        }


        public Builder setColumns(java.lang.String[] projections) {
            this.projections = new ArrayList<String>();
            Collections.addAll(this.projections, projections);
            return this;
        }

        public void setColumns(List<String> projections) {
            this.projections = projections;
        }

//        public Builder setFieldNameValuePair(HashMap<String, Object> fieldNameValuePair) {
//            FieldNameValuePair = fieldNameValuePair;
//            return this;
//        }

        public Builder setColumnNameFieldPair(HashMap<String, Field> columnNameFieldPair) {
            this.columnNameFieldPair = columnNameFieldPair;
            return this;
        }

        public Builder setModelClass(Class<?> cLass) {
            this.modelClass = cLass;
            return this;
        }

        public Builder setNestedTableNameFieldPair(HashMap<String, Field> nestedTableNameFieldPair) {
            this.nestedTableNameFieldPair = nestedTableNameFieldPair;
            return this;
        }

        public SQLiteModel create() {
            return create(null);
        }

        public SQLiteModel create(HashMap<String, Object> fieldNameValuePair) {
            SQLiteModel model = new SQLiteModel() {
                String[] columns;

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String[] getColumns() {
                    if (columns != null) {
                        return columns;
                    }
                    columns = projections.toArray(new String[projections.size()]);
                    return columns;
                }

                @Override
                public String getPrimaryKeyName() {
                    return primaryFieldName;
                }
            };
            if (fieldNameValuePair != null) {
                model.fieldNameValuePair = fieldNameValuePair;
            }
            model.columnNameFieldPair = this.columnNameFieldPair;
            model.nestedTableFieldPair = this.nestedTableNameFieldPair;
            model.modelClass = this.modelClass;
            if (serializer != null) {
                model.serializer = serializer;
            }
            return model;
        }

        public void setContentValueHandler(ContentValueHandler contentValueHandler) {
            if (contentValueHandler == null) {
                contentValueHandler = DEFAULT_CONTAIN_VALUE_HANDLER;
            }
            this.contentValueHandler = contentValueHandler;
        }
    }

    final static SQLiteModel createFromManyToMany(Class<?> parentTableClass, Class<?> childTableClass) throws InstantiationException, IllegalAccessException {
        SQLiteModel parentModel = getSQLiteModel(parentTableClass);
        SQLiteModel childModel = getSQLiteModel(childTableClass);
        String parentTable = parentModel.getName();
        String childTable = childModel.getName();
        Builder builder = new Builder();
        builder.setName(parentTable + "_" + childTable)
                .addColumn("id_" + parentTable)
                .addColumn("id_" + childTable);
        return builder.create();
    }

    private static SQLiteModel getSQLiteModel(Class<?> childTableClass) throws IllegalAccessException, InstantiationException {
        return SQLiteModel.fromClass(childTableClass);
    }

    @Override
    public boolean equals(Object o) {
        //TODO compare value as SQLite data.
        return super.equals(o);
    }

    /**
     * specify a way to read and fill SQLiteModel from SQLiteDatabase Cursor.
     *
     * @param cursorReader
     */
    public void setCursorReader(CursorReader cursorReader) {
        this.cursorReader = cursorReader;
    }

    Serializer serializer = DEFAULT_SERIALIZER;
    CursorReader cursorReader = DEFAULT_CURSOR_READER;
    ContentValueHandler contentValueHandler = DEFAULT_CONTAIN_VALUE_HANDLER;


    static final CursorReader DEFAULT_CURSOR_READER = new CursorReader() {
        @Override
        public void onReadCursor(SQLiteModel model, Cursor c) {
            for (String projection : model.getColumns()) {
                if (projection != null) {
                    int columnIndex = c.getColumnIndex(projection);
                    if (columnIndex >= 0) {
                        String values = c.getString(columnIndex);
                        if (!TextUtils.isEmpty(values)) {
                            model.set(projection, values);
//                            DEFAULT_SERIALIZER.onDeSerialize(values,model.getField(projection))
                        }
                    }
                }
            }
        }
    };
    static final Serializer DEFAULT_SERIALIZER = new Serializer() {
        @Override
        public String onSerialize(Object value, String fieldName) {
            if (value.getClass().isAssignableFrom(CharSequence.class) || value.getClass().isAssignableFrom(String.class)
                    || value.getClass().isAssignableFrom(Double.class) || value.getClass().isAssignableFrom(double.class)
                    || value.getClass().isAssignableFrom(Float.class) || value.getClass().isAssignableFrom(float.class)
                    || value.getClass().isAssignableFrom(Long.class) || value.getClass().isAssignableFrom(long.class)
                    || value.getClass().isAssignableFrom(Boolean.class) || value.getClass().isAssignableFrom(boolean.class)
                    || value.getClass().isAssignableFrom(Integer.class) || value.getClass().isAssignableFrom(int.class)) {
                return value.toString();
            }
            return GSON_SERIALIZER.onSerialize(value, fieldName);
        }

        @Override
        public Object onDeSerialize(String serialized, Field field) {
            try {
                if (field.getType().isAssignableFrom(CharSequence.class) || field.getType().isAssignableFrom(String.class)) {
                    return serialized;
                } else if (field.getType().isAssignableFrom(Double.class) || field.getType().isAssignableFrom(double.class)) {
                    return Double.valueOf(serialized);
                } else if (field.getType().isAssignableFrom(Float.class) || field.getType().isAssignableFrom(float.class)) {
                    return Float.valueOf(serialized);
                } else if (field.getType().isAssignableFrom(Long.class) || field.getType().isAssignableFrom(long.class)) {
                    return Long.valueOf(serialized);
                } else if (field.getType().isAssignableFrom(Boolean.class) || field.getType().isAssignableFrom(boolean.class)) {
                    return Boolean.valueOf(serialized);
                } else if (field.getType().isAssignableFrom(Integer.class) || field.getType().isAssignableFrom(int.class)) {
                    return Integer.valueOf(serialized);
                } else {
                    return GSON_SERIALIZER.onDeSerialize(serialized, field);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    };
    static final Serializer GSON_SERIALIZER = new Serializer() {
        @Override
        public String onSerialize(Object value, String fieldName) {
            String out;
            Gson gson = new Gson();
            Type type = value.getClass();
            out = gson.toJson(value, type);
            return out;
        }

        @Override
        public Object onDeSerialize(String serialized, Field field) {
            try {
                Gson gson = new Gson();
                Type type = Toolkit.getFieldType(field);
                Log.d("asInstance", "stringularProperty=" + serialized);
                Object obj = gson.fromJson(serialized, type);
                return obj;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    };


    static ContentValueHandler DEFAULT_CONTAIN_VALUE_HANDLER = new ContentValueHandler() {
        @Override
        public ContentValues toContentValues(SQLiteModel model) {
            ContentValues pairs = new ContentValues();
            String[] columns = model.getColumns();
            for (String column : columns) {
                if (column != null) {
                    String values = model.getSerializedValue(column);
                    if (column.equals(model.getPrimaryKeyName())) {
                        //TODO find a better comparison.
                        if ((model.getPrimaryKeyPolicy() == PrimaryKey.POLICY_AUTO_INCREMENT
                                || model.getPrimaryKeyPolicy() == PrimaryKey.POLICY_DEFAULT)
                                && "0".equals(values)) {
                            //Do nothing id=0 should autoIncremented.
                            Log.d("SQLiteModel", "toContentValues:" + column + " is primary key should be autoIncremented.");
                        } else if (model.getPrimaryKeyPolicy() == PrimaryKey.POLICY_AUTO_GENERATE) {
                            if (TextUtils.isEmpty(values)) {
                                values = UUID.randomUUID().toString();
                            } else if ("0".equals(values)) {
                                values = "" + (System.currentTimeMillis() + (int) (Math.random() * 100));
                            }
                            pairs.put(column, values);
                        } else {
                            pairs.put(column, values);
                        }
                    } else {
//                        if (values == null) {
//                            continue;
//                        }
                        pairs.put(column, values);
                    }
                }
            }
            return pairs;
        }
    };

    public boolean isCollection() {
        return Collection.class.isAssignableFrom(this.modelClass);
    }

    public static boolean isCollection(Class<?> cLass) {
        return Collection.class.isAssignableFrom(cLass);
    }

    public interface Serializer<T> {
        String onSerialize(T obj, String fieldName);

        Object onDeSerialize(String serialized, Field field);
    }

    public interface CursorReader {
        void onReadCursor(SQLiteModel model, Cursor cursor);
    }

    public interface ContentValueHandler {
        ContentValues toContentValues(SQLiteModel model);

    }

    public <T> List<T> asCollection() {
        List<T> list = new ArrayList();
//        try {
        Object collection = get(TAG_ITEMS);
        Collection<T> collection1 = (Collection<T>) collection;
        for (T obj : collection1) {
            list.add(obj);
        }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return list;
    }

    public Iterator<?> iterator() {
        Object collection = get(TAG_ITEMS);
        if (collection != null) {
            try {
                return ((Collection<?>) collection).iterator();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public <T> T flowInto(T entity, String... columns) throws IllegalAccessException {
        for (String column : columns) {
            Field field = getField(column);
            Object value = get(column);
            field.set(entity, this.serializer.onDeSerialize(String.valueOf(value), field));
        }
        return entity;
    }
}
