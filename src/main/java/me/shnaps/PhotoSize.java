package me.shnaps;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.stream.Stream;

public enum PhotoSize {
    src_xxbig, src_xbig, src_big;

    public static Stream<PhotoSize> stream() {
        return Arrays.stream(values());
    }

    public String optString(final JSONObject o) {
        return o.optString(toString(), null);
    }
}