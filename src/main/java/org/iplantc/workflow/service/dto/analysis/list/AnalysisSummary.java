package org.iplantc.workflow.service.dto.analysis.list;

import java.util.Date;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object representing an analysis summary.
 * 
 * @author psarando
 */
public class AnalysisSummary extends AbstractDto {

    /**
     * The analysis identifier.
     */
    @JsonField(name = "id")
    private String id;

    /**
     * The analysis name.
     */
    @JsonField(name = "name")
    private String name;

    /**
     * The analysis description.
     */
    @JsonField(name = "description")
    private String description;

    /**
     * The name of the analysis integrator.
     */
    @JsonField(name = "integrator_name")
    private String integratorName;

    /**
     * The date when the analysis was integrated.
     */
    @JsonField(name = "integration_date", optional = true)
    private Long integrationDate;

    /**
     * The analysis rating information.
     */
    @JsonField(name = "rating")
    private AnalysisRating rating;

    /**
     * True if the analysis is public.
     */
    @JsonField(name = "is_public")
    private boolean isPublic;

    /**
     * True if the analysis is one of the user's favorites.
     */
    @JsonField(name = "is_favorite")
    private boolean favorite;

    /**
     * True if the analysis is deleted.
     */
    @JsonField(name = "deleted")
    private boolean deleted;

    /**
     * True if the analysis is disabled.
     */
    @JsonField(name = "disabled")
    private boolean disabled;

    /**
     * The group ID of this analysis.
     */
    @JsonField(name = "group_id")
    private String groupId;

    /**
     * The group name of this analysis.
     */
    @JsonField(name = "group_name")
    private String groupName;

    /**
     * @return the analysis description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if the analysis is one of the user's favorites.
     */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * @return the analysis identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the date when the analysis was integrated.
     */
    public Long getIntegrationDate() {
        return integrationDate;
    }

    /**
     * @return the name of the analysis integrator.
     */
    public String getIntegratorName() {
        return integratorName;
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
     * @return the analysis rating information.
     */
    public AnalysisRating getRating() {
        return rating;
    }

    /**
     * @return true if the analysis is deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @return true if the analysis is disabled.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @return the analysis group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @param groupName the analysis group name.
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * @return the analysis group ID.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the analysis group ID.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public AnalysisSummary(AnalysisListing analysis) {
        initializeCommonFields(analysis);

        rating = new AnalysisRating(analysis);
        favorite = false;
    }

    /**
     * @param analysis the analysis represented by this DTO.
     * @param favorites the analysis group containing the user's favorites.
     * @param userRatings the user's analysis ratings.
     */
    public AnalysisSummary(AnalysisListing analysis, Set<AnalysisListing> favorites) {
        initializeCommonFields(analysis);

        rating = new AnalysisRating(analysis);
        favorite = favorites.contains(analysis);
    }

    /**
     * Initializes the fields that are initialized in the same manner for both constructors.
     * 
     * @param analysis the listing for the analysis.
     */
    private void initializeCommonFields(AnalysisListing analysis) {
        id = analysis.getId();
        name = analysis.getName();
        description = StringUtils.defaultString(analysis.getDescription());
        integratorName = StringUtils.defaultString(analysis.getIntegratorName());
        integrationDate = dateAsLong(analysis.getIntegrationDate());
        deleted = analysis.isDeleted();
        disabled = analysis.isDisabled();
    }

    /**
     * Converts a date to a Long.
     * 
     * @param date the date.
     * @return a Long instance containing the number of milliseconds since the epoch.
     */
    private Long dateAsLong(Date date) {
        return date == null ? null : new Long(date.getTime());
    }
}
