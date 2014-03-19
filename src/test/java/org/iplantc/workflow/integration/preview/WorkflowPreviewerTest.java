package org.iplantc.workflow.integration.preview;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.preview.WorkflowPreviewer.
 * 
 * @author Dennis Roberts
 */
public class WorkflowPreviewerTest {

    /**
     * The factory used to generate mock data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The workflow previewer instance being tested.
     */
    private WorkflowPreviewer previewer;

    /**
     * Initializes each of the tests.
     */
    @Before
    public void initialize() {
        daoFactory = new MockDaoFactory();
        daoFactory.setMockPropertyTypeDao(UnitTestUtils.createMockPropertyTypeDao());
        daoFactory.setMockRuleTypeDao(UnitTestUtils.createMockRuleTypeDao());
        daoFactory.setMockInfoTypeDao(UnitTestUtils.createMockInfoTypeDao());
        daoFactory.setMockDataFormatDao(UnitTestUtils.createMockDataFormatDao());
        previewer = new WorkflowPreviewer(daoFactory);
    }

    /**
     * Verifies that the previewer correctly handles a template with an info parameter.
     * 
     * @throws IOException if the JSON string can't be retrieved from the file.
     * @throws JSONException if the JSON is invalid or doesn't meet the expectations of the previewer.
     */
    @Test
    public void shouldHandleTemplateWithInfoParameter() throws IOException, JSONException {
        String jsonString = IOUtils.toString(getClass().getResourceAsStream("/json/template_with_info_parameter.json"));
        JSONObject result = previewer.previewTemplate(new JSONObject(jsonString));
        JSONArray analyses = result.getJSONArray("analyses");
        assertEquals(1, analyses.length());

        JSONObject analysis = analyses.getJSONObject(0);
        JSONArray groups = analysis.getJSONArray("groups");
        assertEquals(1, groups.length());

        JSONObject group = groups.getJSONObject(0);
        JSONArray properties = group.getJSONArray("properties");
        assertEquals(1, properties.length());

        JSONObject property = properties.getJSONObject(0);
        assertEquals("Info", property.getString("type"));
        assertEquals("this is a static test", property.getString("label"));
    }
}
