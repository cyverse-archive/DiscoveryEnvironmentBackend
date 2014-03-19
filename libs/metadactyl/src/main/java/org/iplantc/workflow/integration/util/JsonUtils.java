package org.iplantc.workflow.integration.util;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility methods for dealing with JSON objects.
 *
 * @author Dennis Roberts
 */
public class JsonUtils {

    /**
     * Gets an optional string field value from a JSON object using multiple possible keys. The first key that exists
     * in the JSON object is used.
     *
     * @param json the JSON object.
     * @param defaultValue the default value for the property.
     * @param keys the list of possible keys.
     * @return the value of the first matching key or the default value if no match is found.
     * @throws JSONException if the value of a matching field is not a string.
     */
    public static String optString(JSONObject json, String defaultValue, String... keys) throws JSONException {
        String value = defaultValue;
        for (String key : keys) {
            if (json.has(key)) {
                value = json.getString(key);
                break;
            }
        }
        return value;
    }

    /**
     * Gets an optional string field from a JSON object using multiple possible keys. The first key that exists and
     * has a non-empty value in the JSON object is used.
     *
     * @param json the JSON object.
     * @param defaultValue the default value for the property.
     * @param keys the list of possible keys.
     * @return the value of the first matching key with a non-empty value or the default value if no match is found.
     * @throws JSONException if the value of a matching field is not a string.
     */
    public static String nonEmptyOptString(JSONObject json, String defaultValue, String... keys) throws JSONException {
        String value = defaultValue;
        for (String key : keys) {
            if (json.has(key)) {
                value = json.getString(key);
                if (!StringUtils.isEmpty(value)) {
                    break;
                }
            }
        }
        return value;
    }

    /**
     * Gets an optional boolean field value from a JSON object using multiple possible keys. The first key that exists
     * in the JSON object is used.
     *
     * @param json the JSON object.
     * @param defaultValue the default value for the property.
     * @param keys the list of possible keys.
     * @return the value of the first matching key or the default value if no match is found.
     * @throws JSONException if the value of a matching field is not a boolean value.
     */
    public static boolean optBoolean(JSONObject json, boolean defaultValue, String... keys) throws JSONException {
        boolean value = defaultValue;
        for (String key : keys) {
            if (json.has(key)) {
                value = json.getBoolean(key);
                break;
            }
        }
        return value;
    }

    /**
     * Puts a value in a JSON object if the value is not null.
     *
     * @param <T> the type of value being placed in the JSON object.
     *
     * @param json the JSON object.
     * @param key the key to associate with the value.
     * @param value the value to place in the JSON object.
     * @throws JSONException if a JSON error occurs.
     */
    public static <T> void putIfNotNull(JSONObject json, String key, T value) throws JSONException {
        if (value != null) {
            json.put(key, value);
        }
    }

    /**
     * Puts a value in a JSON array if the value is not null.
     *
     * @param <T> the type of value being placed in the JSON array.
     * @param array the JSON array.
     * @param value the value to place in the JSON array.
     * @throws JSONException if a JSON error occurs.
     */
    public static <T> void putIfNotNull(JSONArray array, T value) throws JSONException {
        if (value != null) {
            array.put(value);
        }
    }

    /**
     * Puts a JSON array in a JSON object if the array is not null or empty.
     *
     * @param json the JSON object.
     * @param key the key to associate with the array.
     * @param value the JSON array.
     * @throws JSONException if a JSON error occurs.
     */
    public static void putIfNotEmpty(JSONObject json, String key, JSONArray value) throws JSONException {
        if (!isEmpty(value)) {
            json.put(key, value);
        }
    }

    /**
     * @param value the JSON array.
     * @return true if the given array is empty or null.
     */
    public static boolean isEmpty(JSONArray value) {
        return value == null || value.length() < 1;
    }

    /**
     * Converts a map to a JSON object.
     *
     * @param <T> the type of the values contained in the map.
     * @param map the map.
     * @return the JSON object.
     * @throws JSONException if a JSON error occurs.
     */
    public static <T> JSONObject mapToJsonObject(Map<String, T> map) throws JSONException {
        JSONObject json = new JSONObject();
        if (map != null) {
            for (String key : map.keySet()) {
                json.put(key, map.get(key));
            }
        }
        return json;
    }

    /**
     * Converts a collection to a JSON array.
     *
     * @param <T> the type of the elements contained in the collection.
     * @param collection the collection.
     * @return the JSON array.
     * @throws JSONException if a JSON error occurs.
     */
    public static <T> JSONArray collectionToJsonArray(Collection<T> collection) throws JSONException {
        JSONArray array = new JSONArray();
        if (collection != null) {
            for (T element : collection) {
                array.put(element);
            }
        }
        return array;
    }

    /**
     * Converts an instance of org.json.JSONObject to an instance of net.sf.json.JSONObject.
     *
     * @param json the original JSON object.
     * @return the converted JSON object.
     */
    public static net.sf.json.JSONObject toNetSfJsonObject(JSONObject json) {
        return net.sf.json.JSONObject.fromObject(json.toString());
    }
}
