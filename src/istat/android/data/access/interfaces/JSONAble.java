package istat.android.data.access.interfaces;

import org.json.JSONObject;

public  interface JSONAble {
	public void fillFromJSONObject(JSONObject json);

	public JSONObject toJSONObject();
}
