package org.iplantc.workflow.integration.util;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

/**
 * Utility methods for importing analyses, deployed components and templates.
 *
 * @author Dennis Roberts
 */
public class ImportUtils {

    /**
     * Prevent instantiation.
     */
    private ImportUtils() {}

    /**
     * Generates an object identifier.
     *
     * @return the object identifier.
     */
    public static String generateId() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * Obtains an object identifier from a JSON object and generates a new identifier if none is provided.
     *
     * @param json the JSON object.
     * @param fieldName the name of the field containing the identifier.
     * @return the specified identifier or a newly generated identifier.
     */
    public static String getId(JSONObject json, String fieldName) {
        String id = json.optString(fieldName, null);
        if (StringUtils.isEmpty(id)) {
            id = generateId();
        }
        return id;
    }
}
