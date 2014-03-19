package org.iplantc.workflow.service.dto;

import net.sf.json.JSONObject;
import org.iplantc.workflow.core.TransformationActivity;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONArray;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.CategorizedAnalysis.
 * 
 * @author Dennis Roberts
 */
public class CategorizedAnalysisTest {

    /**
     * Verifies that we can construct a DTO from a list of categories and an analysis.
     */
    @Test
    public void testConstructionFromAnalysis() {
        List<String> categories = Arrays.asList("foo", "bar", "baz");
        TransformationActivity analysis = UnitTestUtils.createAnalysis("blrfl");
        CategorizedAnalysis dto = new CategorizedAnalysis("somebody", categories, analysis);
        assertEquals(categories, dto.getCategoryPath().getPath());
        assertEquals("somebody", dto.getCategoryPath().getUsername());
        assertEquals("blrflid", dto.getAnalysis().getId());
        assertEquals("blrfl", dto.getAnalysis().getName());
    }

    /**
     * Verifies that we can construct a DTO from a JSON object.
     */
    @Test
    public void testConstructionFromJson() {
        CategorizedAnalysis dto = new CategorizedAnalysis(createJson("somebody", "foo", "bar", "baz", "blrfl"));
        assertEquals("somebody", dto.getCategoryPath().getUsername());
        assertEquals(Arrays.asList("bar", "baz", "blrfl"), dto.getCategoryPath().getPath());
        assertEquals("fooid", dto.getAnalysis().getId());
        assertEquals("foo", dto.getAnalysis().getName());
    }

    /**
     * Verifies that we can construct a DTO from a JSON string.
     */
    @Test
    public void testConstructionFromString() {
        CategorizedAnalysis dto = new CategorizedAnalysis(createJson("nobody", "quux", "foo", "bar", "baz").toString());
        assertEquals("nobody", dto.getCategoryPath().getUsername());
        assertEquals(Arrays.asList("foo", "bar", "baz"), dto.getCategoryPath().getPath());
        assertEquals("quuxid", dto.getAnalysis().getId());
        assertEquals("quux", dto.getAnalysis().getName());
    }

    /**
     * Verifies that we can convert an analysis DTO to a JSON object.
     */
    @Test
    public void testJsonCreation() {
        List<String> categories = Arrays.asList("foo", "bar", "baz");
        TransformationActivity analysis = UnitTestUtils.createAnalysis("blrfl");
        JSONObject json = new CategorizedAnalysis("nobody", categories, analysis).toJson();
        assertEquals("nobody", json.getJSONObject("category_path").getString("username"));
        assertEquals(categories, json.getJSONObject("category_path").getJSONArray("path"));
        assertEquals("blrflid", json.getJSONObject("analysis").getString("id"));
        assertEquals("blrfl", json.getJSONObject("analysis").getString("name"));
    }

    /**
     * Creates a JSON object representing an analysis category.
     * 
     * @param username the name of the user that owns the workspace.
     * @param analysisName the name of the analysis.
     * @param categories the category names.
     * @return the JSON object.
     */
    private JSONObject createJson(String username, String analysisName, String... categories) {
        JSONObject json = new JSONObject();
        json.put("category_path", new CategoryPath(username, Arrays.asList(categories)));
        json.put("analysis", new AnalysisDto(UnitTestUtils.createAnalysis(analysisName)).toJson());
        return json;
    }
}
