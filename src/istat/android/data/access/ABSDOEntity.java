package istat.android.data.access;

import istat.android.data.access.interfaces.Queryable;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public abstract class ABSDOEntity implements Queryable, Cloneable {

	@Override
	public long insert(SQLiteDatabase db) {
		return db.insert(getEntityName(), null, toContentValues());
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
		ContentValues paire = new ContentValues();
		List<String> keySet = JSONArrayToStringList(json.names());
		if (keySet.size() > 0) {
			for (String tmp : keySet) {
				String value = json.optString(tmp);
				if (!TextUtils.isEmpty(value)) {
					paire.put(tmp, value);
				}
			}
		}

		return paire;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
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
