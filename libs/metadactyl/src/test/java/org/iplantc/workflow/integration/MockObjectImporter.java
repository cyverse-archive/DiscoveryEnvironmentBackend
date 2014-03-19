package org.iplantc.workflow.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.iplantc.workflow.integration.util.ImportUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A mock object importer used for testing.
 *
 * @author Dennis Roberts
 */
public class MockObjectImporter implements ObjectImporter {

    /**
     * The objects that have been imported.
     */
    private List<JSONObject> importedObjects = new LinkedList<JSONObject>();

    /**
     * The arrays that have been imported.
     */
    private List<JSONArray> importedArrays = new LinkedList<JSONArray>();

    /**
     * Indicates what should be done if an existing object matches the one being imported.
     */
    private UpdateMode updateMode = UpdateMode.DEFAULT;

    /**
     * @return the list of imported objects.
     */
    public List<JSONObject> getImportedObjects() {
        return Collections.unmodifiableList(importedObjects);
    }

    /**
     * @return the list of imported arrays.
     */
    public List<JSONArray> getImportedArrays() {
        return Collections.unmodifiableList(importedArrays);
    }

    /**
     * Enables the replacement of existing objects.
     */
    @Override
    public void enableReplacement() {
        setUpdateMode(UpdateMode.REPLACE);
    }

    /**
     * Disables the replacement of existing objects.
     */
    @Override
    public void disableReplacement() {
        setUpdateMode(UpdateMode.THROW);
    }

    /**
     * Instructs the importer to ignore attempts to replace existing objects.
     */
    @Override
    public void ignoreReplacement() {
        setUpdateMode(UpdateMode.IGNORE);
    }

    /**
     * Explicitly sets the update mode.
     *
     * @param updateMode the new update mode.
     */
    @Override
    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    /**
     * @return the current update mode.
     */
    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String importObject(JSONObject json) throws JSONException {
        importedObjects.add(json);
        return ImportUtils.generateId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> importObjectList(JSONArray array) throws JSONException {
        importedArrays.add(array);
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++) {
            result.add(ImportUtils.generateId());
        }
        return result;
    }
}
