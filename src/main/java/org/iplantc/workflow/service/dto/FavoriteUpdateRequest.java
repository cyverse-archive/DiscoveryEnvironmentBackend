package org.iplantc.workflow.service.dto;

/**
 * A data transfer object used to update a user's favorites group.
 * 
 * @author Dennis Roberts
 */
public class FavoriteUpdateRequest extends AbstractDto {

    /**
     * The user's workspace ID.
     */
    @JsonField(name = "workspace_id")
    protected long workspaceId;

    /**
     * The identifier of the analysis to add to or remove from the favorites group.
     */
    @JsonField(name = "analysis_id")
    protected String analysisId;

    /**
     * True if the analysis should be added to the user's favorites.
     */
    @JsonField(name = "user_favorite")
    protected boolean favorite;

    /**
     * @return the analysis identifier.
     */
    public String getAnalysisId() {
        return analysisId;
    }

    /**
     * @return true if the analysis should be added to the user's favorites.
     */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * @return the user's workspace identifier.
     */
    public long getWorkspaceId() {
        return workspaceId;
    }

    /**
     * @param workspaceId the user's workspace ID.
     * @param analysisId the analysis ID.
     * @param favorite true if the analysis should be added to the user's favorites.
     */
    public FavoriteUpdateRequest(long workspaceId, String analysisId, boolean favorite) {
        this.workspaceId = workspaceId;
        this.analysisId = analysisId;
        this.favorite = favorite;
    }

    /**
     * Generates a favorite update request from the given string.
     * 
     * @param str the string to build the JSON request from.
     */
    public FavoriteUpdateRequest(String str) {
        fromString(str);
    }
}
