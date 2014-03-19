package org.iplantc.workflow.util;

import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

/**
 * Utility methods for dealing with JSON objects using the Source Forge library.
 * 
 * @author Dennis Roberts
 */
public class SfJsonUtils {

    // Prevent instantiation.
    private SfJsonUtils() {}

    /**
     * Returns true if an object is null or an instance of JSONNull.
     *
     * @param o the object.
     * @return true if the object is null or an instance of JSONNull. False, otherwise.
     */
    private static boolean isNull(Object o) {
        return o == null || o instanceof JSONNull;
    }

    /**
     * Provides access to a string field that can have one of several names.
     * 
     * @param json the JSON object.
     * @param defaultValue the default field value.
     * @param keys the field names.
     * @return the field value if it's found or the default value if the field isn't found.
     */
    public static String optString(JSONObject json, String defaultValue, String... keys) {
        for (String key : keys) {
            if (json.containsKey(key)) {
                Object val = json.get(key);
                if (!isNull(val)) {
                    return val.toString();
                }
            }
        }
        return defaultValue;
    }

    /**
     * Obtains a string from a JSON object. If the specified key doesn't exist in the JSON object or the key does
     * exist but is associated with a null value then specified default value is returned.
     *
     * @param json the JSON object.
     * @param key the key to search for in the JSON object.
     * @param defaultValue the default field value.
     * @return the value of the key in the JSON object or the default value if the value associated with the key
     *         doesn't exist or is null.
     */
    public static String defaultString(JSONObject json, String key, String defaultValue) {
        if (!json.containsKey(key)) {
            return defaultValue;
        }
        Object value = json.get(key);
        if (isNull(value)) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * Obtains a string from a JSON object. If the specified key doesn't exist in the JSON object or the key does
     * exist but is associated with a null value then the empty string is returned.
     *
     * @param json the JSON object.
     * @param key the key to search for in the JSON object.
     * @return the value of the key in the JSON object or the default value if the value associated with the key
     *         does not exist or is null.
     */
    public static String defaultString(JSONObject json, String key) {
        return defaultString(json, key, "");
    }

    /**
     * Returns true if the JSON object contains the specified key and the value associated with the key is not
     * null.
     *
     * @param json the JSON object.
     * @param key the key.
     * @return true if the key exists and is not associated with a null value. False, otherwise.
     */
    public static boolean contains(JSONObject json, Object key) {
        return json.containsKey(key) && !isNull(json.get(key));
    }
}
