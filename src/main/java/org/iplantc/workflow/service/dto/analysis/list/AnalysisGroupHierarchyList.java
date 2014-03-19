package org.iplantc.workflow.service.dto.analysis.list;

import java.util.ArrayList;
import java.util.List;
import org.iplantc.persistence.dto.listing.AnalysisGroup;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object representing a list of analysis group hierarchy listings.
 * 
 * @author Dennis Roberts
 */
public class AnalysisGroupHierarchyList extends AbstractDto {

    /**
     * The list of analysis group hierarchy listings.
     */
    @JsonField(name = "groups")
    private List<AnalysisGroupHierarchy> groups;

    /**
     * @return the list of analysis group hierarchy listings.
     */
    public List<AnalysisGroupHierarchy> getGroups() {
        return groups;
    }

    /**
     * @param analysisGroups the list of analysis groups.
     * @param daoFactory used to obtain data access objects.
     */
    public AnalysisGroupHierarchyList(List<AnalysisGroup> analysisGroups, DaoFactory daoFactory) {
        groups = new ArrayList<AnalysisGroupHierarchy>();
        for (AnalysisGroup analysisGroup : analysisGroups) {
            groups.add(new AnalysisGroupHierarchy(analysisGroup, daoFactory));
        }
    }
}
