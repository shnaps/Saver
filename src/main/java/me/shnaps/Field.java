package me.shnaps;


import org.json.JSONArray;
import org.json.JSONObject;

public enum Field {
    wall, attachments, photo;

    public JSONObject optJSONObject(final JSONObject o) {
        return o.optJSONObject(toString());
    }

    public JSONArray optJSONArray(final JSONObject o) {
        return o.optJSONArray(toString());
    }
}