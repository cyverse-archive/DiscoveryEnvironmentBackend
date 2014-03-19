package org.iplantc.workflow.dao;

import java.util.List;
import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.core.Rating;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public interface RatingDao extends GenericDao<Rating> {
    /**
     * Gets the average of votes for a TransformationActivity
     * 
     * @param transformationActivity
     *  TransformationActivity to get votes for.
     * @return 
     *  Number of votes.
     */
    public Double getVoteAverageForTransformationActivity(TransformationActivity transformationActivity);

    /**
     * Finds a vote a user has cast for a certain TransformationActivity.
     * @param user
     *  User who voted.
     * @param transformationActivity 
     *  TransformationActivity voted for.
     * @return 
     *  The vote or null if no vote was found.
     */
    public Rating findByUserAndTransformationActivity(User user, TransformationActivity transformationActivity);

    /**
     * Finds all the ratings a user has made.
     * 
     * @param user
     *  User to search for.
     * @return 
     *  List of all that user's ratings.
     */
    public List<Rating> findByUser(User user);
}
