package org.iplantc.workflow.service.dto.analysis.list;

import java.util.Map;

import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object representing an analysis rating.
 * 
 * @author Dennis Roberts
 */
public class AnalysisRating extends AbstractDto {

    /**
     * The average rating for the analysis.
     */
    @JsonField(name = "average")
    private double average;

    /**
     * The rating assigned to the analysis by the current user.
     */
    @JsonField(name = "user", optional = true)
    private Integer user;

    /**
     * The Confluence ID of a comment on the analysis by the current user.
     */
    @JsonField(name = "comment_id", optional = true, defaultValue = "")
    private Long commentId;

    /**
     * @return the average rating.
     */
    public double getAverage() {
        return average;
    }

    /**
     * @return the rating assigned to the analysis by the user or null if the user hasn't rated the
     *         analysis.
     */
    public Integer getUser() {
        return user;
    }

    /**
     * @return the comment created by the user or null if the user hasn't commented on the analysis.
     */
    public Long getCommentId() {
        return commentId;
    }

    /**
     * A package-private constructor for use with unit tests.
     * 
     * @param average the average rating.
     * @param user the user.
     */
    AnalysisRating(double average, Integer user) {
        this.average = average;
        this.user = user;
    }

    /**
     * @param json a JSON object representing the analysis rating.
     */
    public AnalysisRating(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str a JSON string representing the analysis rating.
     */
    public AnalysisRating(String str) {
        fromString(str);
    }

    public AnalysisRating(AnalysisListing analysis) {
        this.average = analysis.getAverageRating();
        this.user = null;
    }

    /**
     * @param analysis the analysis that these rating values apply to.
     * @param userRatings the user's analysis ratings and comment IDs.
     */
    public AnalysisRating(AnalysisListing analysis, Map<Long, UserRating> userRatings) {
        this.average = analysis.getAverageRating();
        UserRating userRating = userRatings.get(analysis.getHid());
        if (userRating != null) {
            this.user = userRating.userRating;
            this.commentId = userRating.commentId;
        }
    }
}
