package org.iplantc.workflow.service.dto.analysis.list;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iplantc.persistence.dto.listing.AnalysisGroup;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * A data transfer object representing a list of analysis groups.
 * 
 * @author Dennis Roberts
 */
public class AnalysisGroupList extends AbstractDto {

    /**
     * The list of analysis groups.
     */
    @JsonField(name = "groups")
    private List<AnalysisGroupDto> groups;

    /**
     * @return the list of analysis groups.
     */
    public List<AnalysisGroupDto> getGroups() {
        return groups;
    }

    /**
     * @param templateGroups the list of template groups being marshaled.
     */
    public AnalysisGroupList(List<AnalysisGroup> analysisGroups) {
        this(analysisGroups, new HashSet<AnalysisListing>(), new HashMap<Long, UserRating>());
    }

    /**
     * @param analysisGroups the list of template groups being marshaled.
     * @param favorites the template group containing the user's favorites.
     * @param userRatings the user's rating and comment ID in the wiki.
     */
    public AnalysisGroupList(List<AnalysisGroup> analysisGroups, final Set<AnalysisListing> favorites,
            final Map<Long, UserRating> userRatings) {
        groups = ListUtils.map(new Lambda<AnalysisGroup, AnalysisGroupDto>() {
            @Override
            public AnalysisGroupDto call(AnalysisGroup arg) {
                return new AnalysisGroupDto(arg, favorites, userRatings);
            }
        }, analysisGroups);
    }
}
