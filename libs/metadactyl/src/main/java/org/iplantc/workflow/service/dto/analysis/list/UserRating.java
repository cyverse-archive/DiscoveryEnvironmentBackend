package org.iplantc.workflow.service.dto.analysis.list;

/**
 * Represents an invidual user's rating of an app.
 * 
 * @author hariolf
 * 
 */
public class UserRating {
    Integer userRating;
    Long commentId;

    /**
     * Creates a new UserRating.
     * 
     * @param userRating the numerical rating (number of stars)
     * @param commentId a textual comment
     */
    public UserRating(Integer userRating, Long commentId) {
        this.userRating = userRating;
        this.commentId = commentId;
    }
}
