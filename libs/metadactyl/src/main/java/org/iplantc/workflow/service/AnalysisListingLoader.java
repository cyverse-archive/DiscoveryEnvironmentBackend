package org.iplantc.workflow.service;

import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.AnalysisNotFoundException;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * Used by services to load analysis listings that are required to exist for the service to complete its task.
 * 
 * @author Dennis Roberts
 */
public class AnalysisListingLoader {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public AnalysisListingLoader(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * Loads an analysis listing from the database, throwing an AnalysisNotFound exception if the analysis doesn't
     * exist.
     * 
     * @param id the analysis ID.
     * @return the analysis listing.
     * @throws AnalysisNotFoundException if the analysis listing can't be found.
     */
    public AnalysisListing load(String id) throws AnalysisNotFoundException {
        AnalysisListing analysis = daoFactory.getAnalysisListingDao().findByExternalId(id);
        if (analysis == null) {
            throw new AnalysisNotFoundException(id);
        }
        return analysis;
    }
}
