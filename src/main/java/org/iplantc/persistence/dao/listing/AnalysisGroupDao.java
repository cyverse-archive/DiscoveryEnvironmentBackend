package org.iplantc.persistence.dao.listing;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.listing.AnalysisGroup;

/**
 * @author Dennis Roberts
 */
public interface AnalysisGroupDao extends GenericDao<AnalysisGroup> {

    /**
     * Finds an analysis group using the external identifier.
     * 
     * @param id the identifier.
     * @return the analysis group or null if the analysis group isn't found.
     */
    public AnalysisGroup findByExternalId(String id);
}
