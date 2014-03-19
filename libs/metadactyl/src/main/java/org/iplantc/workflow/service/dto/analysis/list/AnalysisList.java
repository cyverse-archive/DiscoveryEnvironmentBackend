package org.iplantc.workflow.service.dto.analysis.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object representing a list of analyses.
 * 
 * @author Dennis Roberts
 */
public class AnalysisList extends AbstractDto {

    /**
     * The list of analyses.
     */
    @JsonField(name = "analyses")
    private List<Analysis> analyses = new ArrayList<Analysis>();

    /**
     * @return an unmodifiable copy of the list of analyses.
     */
    public List<Analysis> getAnalyses() {
        return Collections.unmodifiableList(analyses);
    }

    /**
     * Creates an analysis listing containing at most one analysis.  If an analysis with the given identifier
     * exists then the analysis list will contain that analysis.  Otherwise, the analysis list will be empty.
     * 
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the analysis identifier.
     */
    public AnalysisList(DaoFactory daoFactory, String analysisId) {
        AnalysisListing analysisListing = daoFactory.getAnalysisListingDao().findByExternalId(analysisId);
        if (analysisListing != null) {
            analyses.add(new Analysis(analysisListing));
        }
    }
}
