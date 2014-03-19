package org.iplantc.persistence.dao.listing;

import java.util.List;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.persistence.dto.listing.RatingListing;
import org.iplantc.persistence.dto.user.User;

/**
 * A data access object that can be used to obtain analysis rating information.
 * 
 * @author Dennis Roberts
 */
public interface RatingListingDao {

    /**
     * Finds all of the analysis ratings for a given user.
     * 
     * @param user the user.
     * @return the list of analysis ratings.
     */
    public List<RatingListing> findByUser(User user);

    /**
     * Finds the rating listing for a given user and analysis listing.
     * 
     * @param user the user.
     * @param analysisListing the analysis listing.
     * @return the rating listing.
     */
    public RatingListing findByUserAndAnalysisListing(User user, AnalysisListing analysisListing);
}
