package org.iplantc.workflow.integration.json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert JSON documents to objects.
 * 
 * @author Dennis Roberts
 */
public interface TitoUnmarshaller<T> {

    /**
     * Converts a JSON document to a Java Object.
     * 
     * @param json the top-level object in the JSON document.
     * @return the Java object.
     * @throws JSONException if the JSON document doesn't meet the unmarshaller's requirements.
     */
    T fromJson(JSONObject json) throws JSONException;
}
