package org.iplantc.persistence.dao.listing;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.listing.AnalysisListing;

/**
 * @author Dennis Roberts
 */
public interface AnalysisListingDao extends GenericDao<AnalysisListing> {

    /**
     * Finds an analysis listing using the external identifier.
     * 
     * @param id the identifier.
     * @return the analysis listing or null if the analysis listing isn't found.
     */
    public AnalysisListing findByExternalId(String id);
}
