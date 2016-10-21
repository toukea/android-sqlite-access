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

import istat.android.data.access.interfaces.JSONAble;
import istat.android.data.access.interfaces.Queryable;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

abstract class DOEntity extends ABSDOEntity implements JSONAble {
	private HashMap<String, Object> map = new HashMap<String, Object>();
	private AnnotationParser parser;
	protected String tb_name, primary_key;
	protected String[] tb_projection;
	public static String TAG_CLASS = "istat.data.access.DbEntity.class";
	private Object instance;
	List<String> reflectionFieldNames = new ArrayList<String>();

	protected DOEntity() {
		// TODO Auto-generated constructor stub
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

	protected static <T extends DOEntity> void set(String name, T obj,
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

	protected static <T extends DOEntity> Object get(String name, T obj) {
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
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return map;
	}

	@Override
	public JSONObject toJSONObject() {
		// TODO Auto-generated method stub
		JSONObject json = createJsonFromHashMap(map);
		try {
			if (parser == null)
				parser = new AnnotationParser();
			parser.fillJSON(getClass(), json);
			String className = instance.getClass() + "";
			className = className.substring(6, className.length()).trim();
			// Log.e("AAA---3", className);
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
		// TODO Auto-generated method stub
		return toJSONObject().toString();
	}

	@Override
	public ContentValues toContentValues() {
		// TODO Auto-generated method stub
		ContentValues paires = new ContentValues();
		JSONObject toJson = toJSONObject();
		for (String projection : tb_projection) {
			String values = toJson.optString(projection);
			if (!TextUtils.isEmpty(values)) {
				paires.put(projection, values);
			}
		}
		return paires;
	}

	public final void fillFromJSONObject(JSONObject json) {
		// TODO Auto-generated method stub
		onFillFromJson(json);

	}

	@Override
	public final void fillFromCursor(Cursor c) {
		// TODO Auto-generated method stub
		onFillFromCursor(c);

	}

	public final void fillFromPrimaryKey(String primaryKey, SQLiteDatabase db) {
		onFillFromPrimaryKey(primaryKey, db);
	}

	public long merge(SQLiteDatabase db) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		if (exist(db)) {
			return update(db);
		} else {
			return insert(db);
		}
	}

	protected void onPersistEmbadedDbEntity(SQLiteDatabase db, Queryable entity) {
		entity.persist(db);
	}

	protected void onFillFromPrimaryKey(String primaryKey, SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Cursor c = db.query(tb_name, tb_projection, primary_key + "=?",
				new String[] { primaryKey }, null, null, null);
		if (c.getCount() > 0) {
			c.moveToNext();
			fillFromCursor(c);

		}
	}

	protected void onFillFromJson(JSONObject json) {
		// TODO Auto-generated method stub
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
			// e.printStackTrace();

		}
	}

	protected void onFillFromCursor(Cursor c) {
		// TODO Auto-generated method stub
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

	@Override
	public long insert(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		long out = 0;
		// db.beginTransaction();
		try {
			out = super.insert(db);
			persistEmbadedDbEntity(db);
			// db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// db.endTransaction();
		}

		return out;
	}

	public int update(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		int out = 0;
		// db.beginTransaction();
		try {
			out = super.update(db, primary_key + "= ?",
					new String[] { getPrimaryKey() });
			persistEmbadedDbEntity(db);
			// db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// db.endTransaction();
		}

		return out;
	}

	public void merge(DOEntity entity) {
		merge(entity, true, true);
	}

	public void merge(DOEntity entity, boolean override, boolean mergeEmptyValue) {
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
		// TODO Auto-generated method stub
		Cursor c = db.query(tb_name, new String[] { primary_key }, primary_key
				+ "= ?", new String[] { getPrimaryKey() }, null, null, null);
		int count = c.getCount();
		c.close();
		return count > 0;
	}

	public int delete(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		return super.delete(db, primary_key + "= ?",
				new String[] { getPrimaryKey() });
	}

	public static <T extends DOEntity> T fromJson(String json)
			throws InstantiationException, IllegalAccessException,
			JSONException {
		return fromJson(new JSONObject(json), null);
	}

	public static <T extends DOEntity> T fromJson(String json, Class<T> clazz)
			throws InstantiationException, IllegalAccessException,
			JSONException {
		return fromJson(new JSONObject(json), clazz);
	}

	public static <T extends DOEntity> T fromQueryable(JSONAble q)
			throws InstantiationException, IllegalAccessException {
		return fromJson(q.toJSONObject(), q.getClass());
	}

	@SuppressWarnings("unchecked")
	public static <T extends DOEntity> T fromJson(JSONObject json,
			Class<?> clazzs) throws InstantiationException,
			IllegalAccessException {
		if (json == null)
			return null;
		try {
			String clazz = json.optString(TAG_CLASS);
			if (!TextUtils.isEmpty(clazz)) {
				clazzs = Class.forName(clazz);
			} else if (clazzs == null) {
				return null;
			}
			try {
				Object obj = clazzs.getConstructor(JSONObject.class)
						.newInstance(json);
				if (obj instanceof DOEntity)
					return (T) obj;
				else
					return null;
			} catch (Exception e) {
				Object obj = Class.forName(clazz).newInstance();
				if (obj instanceof DOEntity) {
					JSONAble jsonentity = (DOEntity) obj;
					jsonentity.fillFromJSONObject(json);
					if (obj instanceof DOEntity)
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

	private void persistEmbadedDbEntity(SQLiteDatabase db) {
		try {
			Iterator<String> keySet = map.keySet().iterator();
			while (keySet.hasNext()) {
				String tmp = keySet.next();
				Object obj = map.get(tmp);
				if (obj != null && obj instanceof Queryable) {
					onPersistEmbadedDbEntity(db, ((Queryable) obj));
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
					if (obj instanceof JSONAble) {
						JSONAble jsonentity = (JSONAble) obj;
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
				if (obj instanceof JSONAble) {
					JSONAble jsonentity = (JSONAble) obj;
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
		// TODO Auto-generated method stub
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

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DbEntry {
		String info() default "";
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EntityProperty {
		String info() default "";
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PrimaryKey {

	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TableName {

	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Projection {

	}

	private boolean hasPrimaryKey() {
		// TODO Auto-generated method stub
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
						|| field.isAnnotationPresent(DbEntry.class)
						|| field.isAnnotationPresent(EntityProperty.class)) {
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
				if (field.isAnnotationPresent(Projection.class)) {
					try {
						field.setAccessible(true);
						String[] tmp_prj = (String[]) field.get(instance);
						for (String item : tmp_prj) {
							if (!projection.contains(item)) {
								projection.add(item);
							}
						}
					} catch (Exception e) {
						// e.printStackTrace();
					}
				}
				if (field.isAnnotationPresent(TableName.class)) {
					field.setAccessible(true);
					tb_name = field.get(instance) + "";
				}
			}
			tb_projection = new String[projection.size()];
			tb_projection = projection.toArray(tb_projection);

		}

		public void fillJSON(Class<?> clazz, JSONObject json)
				throws IllegalAccessException, IllegalArgumentException,
				JSONException {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {

				if (field.isAnnotationPresent(DbEntry.class)
						|| field.isAnnotationPresent(PrimaryKey.class)
						|| field.isAnnotationPresent(EntityProperty.class)) {
					field.setAccessible(true);
					Object obj = field.get(instance);
					if (obj instanceof JSONAble) {
						JSONAble jsonentity = (JSONAble) obj;
						json.put(field.getName(), jsonentity.toJSONObject());
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
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(DbEntry.class)
						|| field.isAnnotationPresent(PrimaryKey.class)
						|| field.isAnnotationPresent(EntityProperty.class)) {
					field.setAccessible(true);
					Object obj = field.get(instance);
					if (obj != null && !TextUtils.isEmpty(obj + "")) {
						map.put(field.getName(), obj);
					}
				}
			}
		}
	}

}
