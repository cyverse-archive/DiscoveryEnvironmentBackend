package org.iplantc.workflow.integration;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to import objects from JSON objects or arrays.
 *
 * @author Dennis Roberts
 */
public interface ObjectImporter {

    /**
     * Enables replacement of existing objects.
     */
    public void enableReplacement();

    /**
     * Disables replacement of existing objects.
     */
    public void disableReplacement();

    /**
     * Ignores attempts to replace existing objects.
     */
    public void ignoreReplacement();

    /**
     * Explicitly sets the update mode.
     *
     * @param updateMode the new update mode.
     */
    public void setUpdateMode(UpdateMode updateMode);

    /**
     * Imports a single object using information in a JSON object.
     *
     * @param json the JSON object.
     * @return the object ID.
     * @throws JSONException if the JSON object does not meet the expectations of the importer.
     */
    public String importObject(JSONObject json) throws JSONException;

    /**
     * Imports a list of objects using information in a JSON array.
     *
     * @param array the JSON array
     * @return a list of object IDs.
     * @throws JSONException if the JSON array does not meet the expectations of the importer.
     */
    public List<String> importObjectList(JSONArray array) throws JSONException;
}
