package org.iplantc.workflow.service.dto;

import java.util.List;
import java.util.Arrays;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.AnalysisCategoryList.
 * 
 * @author Dennis Roberts
 */
public class AnalysisCategoryListTest {

    /**
     * Verifies that we can construct a DTO from a JSON object.
     */
    @Test
    public void testConstructionFromJson() {
        List<CategorizedAnalysis> categories = new AnalysisCategoryList(createJson()).getCategories();
        assertEquals(2, categories.size());
        assertEquals(createAnalysisCategory("somebody", "quux", "foo", "bar", "baz"), categories.get(0));
        assertEquals(createAnalysisCategory("somebody", "glarb", "foo", "bar", "blrfl"), categories.get(1));
    }

    /**
     * Verifies that we can construct a DTO from a JSON string.
     */
    @Test
    public void testConstructionFromString() {
        List<CategorizedAnalysis> categories = new AnalysisCategoryList(createJson().toString()).getCategories();
        assertEquals(2, categories.size());
        assertEquals(createAnalysisCategory("somebody", "quux", "foo", "bar", "baz"), categories.get(0));
        assertEquals(createAnalysisCategory("somebody", "glarb", "foo", "bar", "blrfl"), categories.get(1));
    }

    /**
     * Verifies that we can convert a DTO to a JSON object.
     */
    @Test
    public void testJsonGeneration() {
        JSONObject expected = createJson();
        assertEquals(expected, new AnalysisCategoryList(expected).toJson());
    }

    /**
     * Creates a JSON object representing an analysis category list for testing.
     * 
     * @return the JSON object.
     */
    private JSONObject createJson() {
        JSONObject json = new JSONObject();
        json.put("categories", createCategoriesJson());
        return json;
    }

    /**
     * Creates the categories JSON array for testing.
     * 
     * @return the JSON array.
     */
    private JSONArray createCategoriesJson() {
        JSONArray array = new JSONArray();
        array.add(createAnalysisCategoryJson("somebody", "quux", "foo", "bar", "baz"));
        array.add(createAnalysisCategoryJson("somebody", "glarb", "foo", "bar", "blrfl"));
        return array;
    }

    /**
     * Creates an analysis category JSON object for testing.
     * 
     * @param username the name of the user.
     * @param analysisName the name of the analysis.
     * @param categories the list of categories.
     * @return the JSON object.
     */
    private JSONObject createAnalysisCategoryJson(String username, String analysisName, String... categories) {
        return createAnalysisCategory(username, analysisName, categories).toJson();
    }

    /**
     * Creates an analysis category for testing.
     * 
     * @param username the name of the user.
     * @param analysisName the name of the analysis.
     * @param categories the list of categories.
     * @return the analysis category.
     */
    private CategorizedAnalysis createAnalysisCategory(String username, String analysisName, String... categories) {
        return new CategorizedAnalysis(username, Arrays.asList(categories), UnitTestUtils.createAnalysis(analysisName));
    }
}
