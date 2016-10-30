package istat.android.data.access.utils;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import android.content.ContentValues;

public class SQLiteModelConverter {
	public final static ContentValues toContentValues(Object obj)
			throws JSONException {
		Gson gson = new Gson();
		String jsonString = gson.toJson(obj);
		JSONObject json = new JSONObject(jsonString);
		return toContentValues(json);
	}

	public final static ContentValues toContentValues(JSONObject json) {
		ContentValues out = new ContentValues();
		Iterator<?> iterator = json.keys();
		while (iterator.hasNext()) {
			String name = iterator.next().toString();
			out.put(name, json.optString(name));
		}
		return out;
	}
}
