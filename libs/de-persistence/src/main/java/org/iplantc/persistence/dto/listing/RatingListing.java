package org.iplantc.persistence.dto.listing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Contains the information required to represent an analysis rating in the analysis listing services.
 * 
 * @author Dennis Roberts
 */
@NamedQueries({
    @NamedQuery(name = "RatingListing.findByAnalysisAndUser", query =
    "from RatingListing where analysisId = :analysisId and userId = :userId"),
    @NamedQuery(name = "RatingListing.findByUser", query = "from RatingListing where userId = :userId")
})
@Entity
@Table(name = "rating_listing")
public class RatingListing {

    /**
     * The rating listing identifier.  We're currently using the row number for the identifier just to satisfy the
     * requirement for an ID field because there's no other single unique field that can be used as an identifier.
     * This should be okay, however, because this is a read-only DTO.  Just to be safe and to avoid confusion, no
     * getter is provided for this field.
     */
    @Id
    private long id;

    /**
     * The internal analysis identifier.
     */
    @Column(name = "analysis_id")
    private long analysisId;

    /**
     * The internal user identifier.
     */
    @Column(name = "user_id")
    private long userId;

    /**
     * The rating provided by the current user, if the user has rated the analysis.
     */
    @Column(name = "user_rating")
    private Integer userRating;

    /**
     * The comment provided by the current user, if the user has commented on the analysis.
     */
    @Column(name = "comment_id")
    private Long commentId;

    /**
     * @return the analysis identifier.
     */
    public long getAnalysisId() {
        return analysisId;
    }

    /**
     * @return the user identifier.
     */
    public long getUserId() {
        return userId;
    }

    /**
     * @return the rating provided by this user, if the user has rated this analysis.
     */
    public Integer getUserRating() {
        return userRating;
    }

    /**
     * @return the rating provided by this user, if the user has rated this analysis.
     */
    public Long getCommentId() {
        return commentId;
    }
}
