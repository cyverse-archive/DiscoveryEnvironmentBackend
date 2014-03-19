package org.iplantc.workflow.integration.json;

import org.json.JSONObject;

/**
 * Used to convert objects to JSON documents.
 * 
 * @author Dennis Roberts
 *
 * @param <T> the type of object being converted.
 */
public interface TitoMarshaller<T> {

    /**
     * Converts a Java object to a JSON document.
     * 
     * @param object the Java object
     * @return a JSON object.
     */
    JSONObject toJson(T object);
}
