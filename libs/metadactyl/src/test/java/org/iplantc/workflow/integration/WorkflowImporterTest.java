package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iplantc.workflow.WorkflowException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.WorkflowImporter.
 * 
 * @author Dennis Roberts
 */
public class WorkflowImporterTest {

    /**
     * The object importer map.
     */
    private Map<String, MockObjectImporter> objectImporterMap;

    /**
     * The workflow importer being tested.
     */
    private WorkflowImporter importer;

    /**
     * Initializes each test.
     */
    @Before
    public void initialize() {
        createObjectImporterMap();
        createImporter();
    }

    /**
     * Creates the workflow importer.
     */
    private void createImporter() {
        importer = new WorkflowImporter();
        for (String key : objectImporterMap.keySet()) {
            importer.addImporter(key, objectImporterMap.get(key));
        }
    }

    /**
     * Creates the object importer map.
     */
    private void createObjectImporterMap() {
        objectImporterMap = new HashMap<String, MockObjectImporter>();
        objectImporterMap.put("foo", new MockObjectImporter());
        objectImporterMap.put("bar", new MockObjectImporter());
        objectImporterMap.put("baz", new MockObjectImporter());
    }

    /**
     * Verifies that we can import a fully specified object.
     * 
     * @throws JSONException if the JOSN object is invalid.
     */
    @Test
    public void testFullySpecifiedObject() throws JSONException {
        String jsonString = "{   \"foo\": [\n"
            + "        {   \"a\": \"b\",\n"
            + "            \"c\": \"d\"\n"
            + "        },\n"
            + "        {   \"e\": \"f\",\n"
            + "            \"g\": \"h\"\n"
            + "        }\n"
            + "    ],\n"
            + "    \"bar\": [\n"
            + "        {   \"i\": \"j\",\n"
            + "            \"k\": \"l\"\n"
            + "        },\n"
            + "        {   \"m\": \"n\",\n"
            + "            \"o\": \"p\"\n"
            + "        }\n"
            + "    ],\n"
            + "    \"baz\": [\n"
            + "        {   \"q\": \"r\",\n"
            + "            \"s\": \"t\"\n"
            + "        },\n"
            + "        {   \"u\": \"v\",\n"
            + "            \"w\": \"x\"\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importWorkflow(json);

        List<JSONArray> fooArrays = objectImporterMap.get("foo").getImportedArrays();
        assertEquals(1, fooArrays.size());
        assertEquals(json.getJSONArray("foo"), fooArrays.get(0));

        List<JSONArray> barArrays = objectImporterMap.get("bar").getImportedArrays();
        assertEquals(1, barArrays.size());
        assertEquals(json.getJSONArray("bar"), barArrays.get(0));

        List<JSONArray> bazArrays = objectImporterMap.get("baz").getImportedArrays();
        assertEquals(1, bazArrays.size());
        assertEquals(json.getJSONArray("baz"), bazArrays.get(0));
    }

    /**
     * Verifies that the importer can handle an empty JSON object.
     * 
     * @throws JSONException if the JSON object passed to the importer is invalid.
     */
    @Test
    public void testEmptyObject() throws JSONException {
        String jsonString = "{}";
        JSONObject json = new JSONObject(jsonString);
        importer.importWorkflow(json);

        List<JSONArray> fooArrays = objectImporterMap.get("foo").getImportedArrays();
        assertEquals(0, fooArrays.size());

        List<JSONArray> barArrays = objectImporterMap.get("bar").getImportedArrays();
        assertEquals(0, barArrays.size());

        List<JSONArray> bazArrays = objectImporterMap.get("baz").getImportedArrays();
        assertEquals(0, bazArrays.size());
    }

    /**
     * Verifies that the importer can handle a partially specified JSON object.
     * 
     * @throws JSONException if the JSON object passed to the importer is invalid.
     */
    @Test
    public void testPartiallySpecifiedObject() throws JSONException {
        String jsonString = "{   \"foo\": [\n"
            + "        {   \"a\": \"b\",\n"
            + "            \"c\": \"d\"\n"
            + "        },\n"
            + "        {   \"e\": \"f\",\n"
            + "            \"g\": \"h\"\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importWorkflow(json);

        List<JSONArray> fooArrays = objectImporterMap.get("foo").getImportedArrays();
        assertEquals(1, fooArrays.size());
        assertEquals(json.getJSONArray("foo"), fooArrays.get(0));

        List<JSONArray> barArrays = objectImporterMap.get("bar").getImportedArrays();
        assertEquals(0, barArrays.size());

        List<JSONArray> bazArrays = objectImporterMap.get("baz").getImportedArrays();
        assertEquals(0, bazArrays.size());
    }

    /**
     * Verifies that an exception is thrown if an unrecognized top-level key is used in the JSON object.
     * 
     * @throws JSONException if the JSON object passed to the importer is invalid.
     */
    @Test(expected = WorkflowException.class)
    public void unrecognizedKeyShouldGenerateException() throws JSONException {
        String jsonString = "{   \"quux\": [\n"
            + "        {   \"y\": \"z\",\n"
            + "            \"a\": \"b\"\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importWorkflow(json);
    }

    /**
     * 
     * @throws IOException
     * @throws JSONException
     * 
     */
    @Test
    public void testPullingValuesOutOfSections() throws IOException, JSONException {
        JSONObject json = getTestJSONObject("workflow_import_section_test");
        WorkflowImporter localImporter = createWorkflowImporterAlmostForReal();
        localImporter.importWorkflow(json);
        MockObjectImporter tmplImp = (MockObjectImporter) localImporter.getImporter("templates");

        assertNotNull(tmplImp);
    }

    /**
     * Verifies that we can enable and disable replacement for all object importers.
     */
    @Test
    public void shouldEnableAndDisableReplacement() {
        assertTrue(replacementDisabledForAllImporters());
        importer.enableReplacement();
        assertTrue(replacementEnabledForAllImporters());
        importer.disableReplacement();
        assertTrue(replacementDisabledForAllImporters());
        importer.ignoreReplacement();
        assertTrue(replacementIgnoredForAllImporters());
    }

    /**
     * Determines whether or not replacements are enabled for all importers in our importer map.
     * 
     * @return true if rpelacements are enabled for all importers.
     */
    private boolean replacementEnabledForAllImporters() {
        boolean replacementEnabled = true;
        for (MockObjectImporter currentImporter : objectImporterMap.values()) {
            if (currentImporter.getUpdateMode() != UpdateMode.REPLACE) {
                replacementEnabled = false;
                break;
            }
        }
        return replacementEnabled;
    }

    /**
     * Determines whether or not replacements are disabled for all importers in our importer map.
     * 
     * @return true if replacements are disabled for all importers.
     */
    private boolean replacementDisabledForAllImporters() {
        boolean replacementDisabled = true;
        for (MockObjectImporter currentImporter : objectImporterMap.values()) {
            if (currentImporter.getUpdateMode() != UpdateMode.THROW) {
                replacementDisabled = false;
                break;
            }
        }
        return replacementDisabled;
    }

    /**
     * Determines whether or not replacements are ignored for all importers in our importer map.
     * 
     * @return true if replacements are ignored for all importers.
     */
    private boolean replacementIgnoredForAllImporters() {
        boolean replacementIgnored = true;
        for (MockObjectImporter currentImporter : objectImporterMap.values()) {
            if (currentImporter.getUpdateMode() != UpdateMode.IGNORE) {
                replacementIgnored = false;
                break;
            }
        }
        return replacementIgnored;
    }

    /**
     * Create an importer with realistic importer key-names.
     * 
     * @return a workflow importer with mock object importers.
     */
    private WorkflowImporter createWorkflowImporterAlmostForReal() {
        WorkflowImporter localImporter = new WorkflowImporter();
        localImporter.addImporter("components", new MockObjectImporter());
        localImporter.addImporter("templates", new MockObjectImporter());
        localImporter.addImporter("notification_sets", new MockObjectImporter());
        localImporter.addImporter("analyses", new MockObjectImporter());
        return localImporter;
    }
}
