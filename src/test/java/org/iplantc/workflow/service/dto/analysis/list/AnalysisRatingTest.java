/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iplantc.workflow.service.dto.analysis.list;

import net.sf.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dennis
 */
public class AnalysisRatingTest {
    
    /**
     * Verify that we can convert the DTO to an analysis object.
     */
    @Test
    public void testJsonProduction() {
        AnalysisRating rating = new AnalysisRating(1.23, 4);
        JSONObject json = rating.toJson();
        assertEquals(1.23, json.getDouble("average"), 0.01);
        assertEquals(4, json.getInt("user"));
    }

    /**
     * Verify that the JSON field for the user rating is omitted if the user rating is null.
     */
    @Test
    public void testJsonForNullUserRating() {
        AnalysisRating rating = new AnalysisRating(1.23, null);
        JSONObject json = rating.toJson();
        assertEquals(1.23, json.getDouble("average"), 0.01);
        assertFalse(json.has("user"));
    }

    /**
     * Verifies that an analysis rating can be constructed from a JSON object.
     */
    @Test
    public void testConstructionFromJsonObject() {
        AnalysisRating rating = new AnalysisRating(createJson(3.21, 5));
        assertEquals(3.21, rating.getAverage(), 0.01);
        assertEquals(5, rating.getUser().intValue());
    }

    /**
     * Create a JSON object representing an analysis rating.
     * 
     * @param average the average rating.
     * @param user the user's rating.
     * @return the JSON object.
     */
    private JSONObject createJson(double average, Integer user) {
        JSONObject json = new JSONObject();
        json.put("average", average);
        json.put("user", user);
        return json;
    }
}
