package org.iplantc.workflow.service.dto.analysis.list;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.listing.AnalysisGroup;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object representing an analysis group.  This DTO is used to list the analysis group hierarchy;
 * sub-groups are listed recursively, but the analyses in the analysis group
 * 
 * @author Dennis Roberts
 */
public class AnalysisGroupHierarchy extends AbstractDto {

    /**
     * The analysis group name.
     */
    @JsonField(name = "name")
    private String name;

    /**
     * The analysis group identifier.
     */
    @JsonField(name = "id")
    private String id;

    /**
     * The analysis group description.
     */
    @JsonField(name = "description")
    private String description;

    /**
     * The list of subgroups.
     */
    @JsonField(name = "groups", optional = true)
    private List<AnalysisGroupHierarchy> subgroups;

    /**
     * The number of templates in the analysis group.
     */
    @JsonField(name = "template_count")
    private int templateCount;

    /**
     * True if the analysis group is public.
     */
    @JsonField(name = "is_public")
    private boolean isPublic;

    /**
     * @return the analysis group description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the analysis group identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return true if the analysis is public.
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @return the analysis name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the list of analysis groups.
     */
    public List<AnalysisGroupHierarchy> getSubgroups() {
        return subgroups;
    }

    /**
     * @return the number of templates in the analysis group.
     */
    public int getTemplateCount() {
        return templateCount;
    }
    
    /**
     * TODO: fix the code used to get the template count.
     * 
     * @param analysisGroup the analysis group that this DTO represents.
     * @param daoFactory used to get data access objects.
     */
    public AnalysisGroupHierarchy(AnalysisGroup analysisGroup, DaoFactory daoFactory) {
        name = analysisGroup.getName();
        id = analysisGroup.getId();
        description = StringUtils.defaultString(analysisGroup.getDescription());
        subgroups = extractSubgroups(analysisGroup, daoFactory);
        templateCount = analysisGroup.getAnalysisCount();
        isPublic = analysisGroup.isPublic();
    }

    /**
     * Extracts the list of subgroups from an analysis group.
     * 
     * @param analysisGroup the analysis group.
     * @param daoFactory used to obtain data access objects.
     * @return the list of subgroups.
     */
    private List<AnalysisGroupHierarchy> extractSubgroups(AnalysisGroup analysisGroup, DaoFactory daoFactory) {
        List<AnalysisGroupHierarchy> result = null;
        if (!analysisGroup.getSubgroups().isEmpty()) {
            result = new ArrayList<AnalysisGroupHierarchy>();
            for (AnalysisGroup subgroup : analysisGroup.getSubgroups()) {
                result.add(new AnalysisGroupHierarchy(subgroup, daoFactory));
            }
        }
        return result;
    }
}
