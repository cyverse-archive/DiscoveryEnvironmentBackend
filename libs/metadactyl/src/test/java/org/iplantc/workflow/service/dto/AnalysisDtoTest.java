package org.iplantc.workflow.service.dto;

import net.sf.json.JSONObject;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.AnalysisDto.
 * 
 * @author Dennis Roberts
 */
public class AnalysisDtoTest {

    /**
     * Verifies that we can construct an analysis DTO from an analysis.
     */
    @Test
    public void testConstructionFromAnalysis() {
        AnalysisDto dto = new AnalysisDto(UnitTestUtils.createAnalysis("analysis"));
        assertEquals("analysisid", dto.getId());
        assertEquals("analysis", dto.getName());
    }

    /**
     * Verifies that we can construct an analysis DTO from a JSON object.
     */
    @Test
    public void testConstructionFromJson() {
        AnalysisDto dto = new AnalysisDto(createJsonObject("fooid", "fooname"));
        assertEquals("fooid", dto.getId());
        assertEquals("fooname", dto.getName());
    }

    /**
     * Verifies that we can construct an analysis DTO from a string.
     */
    @Test
    public void testConstructionFromString() {
        AnalysisDto dto = new AnalysisDto(createJsonObject("barid", "bar").toString());
        assertEquals("barid", dto.getId());
        assertEquals("bar", dto.getName());
    }

    /**
     * Verifies that we can generate JSON for the DTO.
     */
    @Test
    public void testJsonGeneration() {
        JSONObject json = new AnalysisDto(UnitTestUtils.createAnalysis("baz")).toJson();
        assertEquals("bazid", json.getString("id"));
        assertEquals("baz", json.getString("name"));
    }

    /**
     * Creates a JSON object representing an analysis DTO for testing.
     * 
     * @param id the analysis identifier.
     * @param name the analysis name.
     * @return the JSON object.
     */
    private JSONObject createJsonObject(String id, String name) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        return json;
    }
}
