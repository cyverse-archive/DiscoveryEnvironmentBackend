package org.iplantc.workflow.service.dto.analysis.list;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.listing.AnalysisGroup;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * A data transfer object representing an analysis groups.  Analysis groups that are deeper in the hierarchy aren't
 * listed.  Instead, the analyses in those deeper analysis groups are listed directly within this analysis group.
 * 
 * @author Dennis Roberts
 */
public class AnalysisGroupDto extends AbstractDto {

    /**
     * The name of the analysis group.
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
     * The list of analyses within the analysis group or its descendants.
     */
    @JsonField(name = "templates")
    private List<Analysis> analyses;

    /**
     * The number of analyses in the analysis group.
     */
    @JsonField(name = "template_count")
    private int analysisCount;

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
     * @return true if the analysis group is public.
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @return the name of the analysis group.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the number of analyses in the analysis group or its descendants.
     */
    public int getAnalysisCount() {
        return analysisCount;
    }

    /**
     * @return the list of analyses in the analysis group or its descendants.
     */
    public List<Analysis> getAnalyses() {
        return analyses;
    }

    /**
     * @param group the template group represented by this DTO.
     */
    public AnalysisGroupDto(AnalysisGroup group) {
        this.name = group.getName();
        this.id = group.getId();
        this.description = StringUtils.defaultString(group.getDescription());
        this.analyses = extractActiveAnalyses(group);
        this.analysisCount = group.getAnalysisCount();
        this.isPublic = group.isPublic();
    }

    /**
     * @param group the template group represented by this DTO.
     * @param favorites the template group containing the user's favorites.
     * @param userRatings the user's analysis ratings and comment IDs.
     */
    public AnalysisGroupDto(AnalysisGroup group, Set<AnalysisListing> favorites,
            Map<Long, UserRating> userRatings) {
        this.name = group.getName();
        this.id = group.getId();
        this.description = StringUtils.defaultString(group.getDescription());
        this.analyses = extractAnalyses(group, favorites, userRatings);
        this.analysisCount = group.getAnalysisCount();
        this.isPublic = group.isPublic();
    }

    /**
     * Extracts the active analyses from the template group.
     * 
     * @param group the template group represented by this DTO.
     * @return the list of analysis data transfer objects.
     */
    private List<Analysis> extractActiveAnalyses(AnalysisGroup group) {
        return ListUtils.map(new Lambda<AnalysisListing, Analysis>() {
            @Override
            public Analysis call(AnalysisListing arg) {
                return new Analysis(arg);
            }
        }, group.getAllActiveAnalyses());
    }

    /**
     * Extracts the analyses from the template group.
     * 
     * @param group the template group represented by this DTO.
     * @param favorites the template group containing the user's favorites.
     * @param userRatings the user's analysis ratings and comment IDs.
     * @return the list of analysis data transfer objects.
     */
    private List<Analysis> extractAnalyses(AnalysisGroup group, final Set<AnalysisListing> favorites,
            final Map<Long, UserRating> userRatings) {
        return ListUtils.map(new Lambda<AnalysisListing, Analysis>() {
            @Override
            public Analysis call(AnalysisListing arg) {
                return new Analysis(arg, favorites, userRatings);
            }
        }, group.getAllActiveAnalyses());
    }
}
