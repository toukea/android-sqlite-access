package istat.android.data.access.sqlite.interfaces;

import org.json.JSONObject;

public interface JSONable {
    void fillFromJson(JSONObject json);

    JSONObject toJson();
}
