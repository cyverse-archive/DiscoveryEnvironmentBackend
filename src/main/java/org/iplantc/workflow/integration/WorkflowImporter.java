package org.iplantc.workflow.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iplantc.workflow.WorkflowException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A generalized workflow importer. This class imports a workflow that is defined by a single JSON document each top-
 * level field in the JSON document refers to a list of JSON objects that represent a single type of workflow element.
 * Each top-level field is processed by an importer that is specified by the caller. The top-level elements are
 * processed in the order in which the importers are added. For example if an importer is added for "templates" then
 * another workflow is added for "analyses" then workflows will be imported before analyses.
 * 
 * @author Dennis Roberts
 */
public class WorkflowImporter {

    /**
     * Used to map JSON keys to their respective importers.
     */
    private final Map<String, ObjectImporter> importerMap = new HashMap<String, ObjectImporter>();

    /**
     * Used to keep track of the order in which top-level elements are supposed to be processed.
     */
    private final List<String> keysToProcess = new ArrayList<String>();

    /**
     * Adds an importer to the importer map.
     * 
     * @param key the JSON key.
     * @param importer the importer to use for the key.
     */
    public void addImporter(String key, ObjectImporter importer) {
        importerMap.put(key, importer);
        keysToProcess.add(key);
    }

    /**
     * Retrieves the importer associated with the argument key.
     * 
     * @param key the key-name
     * @return the importer associated with the argument key.
     */
    public ObjectImporter getImporter(String key) {
        return importerMap.get(key);
    }

    /**
     * Enables the replacement of existing objects.
     */
    public void enableReplacement() {
        setUpdateMode(UpdateMode.REPLACE);
    }

    /**
     * Disables the replacement of existing objects.
     */
    public void disableReplacement() {
        setUpdateMode(UpdateMode.THROW);
    }

    /**
     * Instructs all of the importers to ignore the replacement of existing objects.
     */
    public void ignoreReplacement() {
        setUpdateMode(UpdateMode.IGNORE);
    }

    /**
     * Explicitly sets the update mode of all importers.
     * 
     * @param updateMode the new update mode.
     */
    public void setUpdateMode(UpdateMode updateMode) {
        for (ObjectImporter importer : importerMap.values()) {
            importer.setUpdateMode(updateMode);
        }
    }

    /**
     * Imports a workflow.
     * 
     * @param json the JSON object representing the workflow.
     * @return
     * @throws JSONException if the JSON object doesn't meet the expectations of this class.
     * @throws WorkflowException if an unrecognized top-level key is received.
     */
    public String importWorkflow(JSONObject json) throws JSONException {
        JSONArray keys = json.names();
        if (keys != null) {
            validateKeys(keys);
            return importWorkflowElements(json);
        }

        return new JSONObject().toString();
    }

    /**
     * Validates the top-level keys in the JSON document.
     * 
     * @param keys the names of the top-level keys.
     * @throws JSONException if one of the keys can't be extracted.
     * @throws WorkflowException if an unrecognized top-level key is found.
     */
    private void validateKeys(JSONArray keys) throws JSONException {
        for (int i = 0; i < keys.length(); i++) {
            String key = keys.getString(i);
            if (importerMap.get(key) == null) {
                throw new WorkflowException("unrecognized top-level JSON key: " + key);
            }
        }
    }

    /**
     * Imports the individual workflow elements.
     * 
     * @param json the JSON object representing all of the workflow elements.
     * @return
     * @throws JSONException if the JSON object doesn't meet the expectations of this class.
     * @throws WorkflowException if an unrecognized top-level key is received.
     */
    private String importWorkflowElements(JSONObject json) throws JSONException {
        JSONObject ret = new JSONObject();
        for (String key : keysToProcess) {
            ObjectImporter importer = importerMap.get(key);
            if (json.has(key)) {
                ret.put(key, importer.importObjectList(json.getJSONArray(key)));
            }
        }

        return ret.toString();
    }
}
