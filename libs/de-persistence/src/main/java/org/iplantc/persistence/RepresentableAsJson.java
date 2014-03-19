package org.iplantc.persistence;

import org.json.JSONObject;

/**
 * Implemented by classes that can be represented as JSON objects.
 * 
 * @author Dennis Roberts
 */
public interface RepresentableAsJson {

    /**
     * Returns a JSON object representing the object.
     * 
     * @return the JSON object.
     */
    public JSONObject toJson();
}
