package istat.android.data.access.interfaces;

import org.json.JSONObject;

public  interface JSONable {
	public void fillFromJson(JSONObject json);

	public JSONObject toJson();
}
