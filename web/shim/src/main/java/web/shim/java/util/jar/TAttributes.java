package web.shim.java.util.jar;

import java.util.LinkedHashMap;
import java.util.Map;

/** Manifest attributes; names are case-insensitive, as in java.util.jar.Attributes. */
public class TAttributes {
    private final Map<String, String> values = new LinkedHashMap<>();

    public String getValue(String name) {
        return values.get(name.toLowerCase());
    }

    public String putValue(String name, String value) {
        return values.put(name.toLowerCase(), value);
    }
}
