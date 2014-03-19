package org.iplantc.workflow.service.dto;

import net.sf.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.FavoriteUpdateRequest.
 * 
 * @author Dennis Roberts
 */
public class FavoriteUpdateRequestTest {

    /**
     * Verifies that the object can be initialized from a JSON string.
     */
    @Test
    public void shouldInitializeFromJsonString() {
        FavoriteUpdateRequest request = new FavoriteUpdateRequest(createJsonString(27, "abc123", true));
        assertEquals(27, request.getWorkspaceId());
        assertEquals("abc123", request.getAnalysisId());
        assertTrue(request.isFavorite());
    }

    /**
     * Verifies that the object can be converted to a JSON object.
     */
    @Test
    public void shouldConvertToJsonObject() {
        FavoriteUpdateRequest request = new FavoriteUpdateRequest(42, "quux", false);
        JSONObject json = request.toJson();
        assertEquals(42, json.getLong("workspace_id"));
        assertEquals("quux", json.getString("analysis_id"));
        assertFalse(request.isFavorite());
    }

    /**
     * Creates a JSON string for testing.
     * 
     * @param workspaceId the workspace ID.
     * @param analysisId the analysis ID.
     * @param favorite true if the object should be placed in the user's workspace.
     * @return the JSON string.
     */
    private String createJsonString(int workspaceId, String analysisId, boolean favorite) {
        JSONObject json = new JSONObject();
        json.put("workspace_id", workspaceId);
        json.put("analysis_id", analysisId);
        json.put("user_favorite", favorite);
        return json.toString();
    }
}
