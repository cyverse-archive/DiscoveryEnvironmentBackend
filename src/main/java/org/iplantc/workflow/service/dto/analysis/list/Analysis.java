package org.iplantc.workflow.service.dto.analysis.list;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;
import org.iplantc.workflow.service.dto.pipelines.AnalysisValidationDto;

/**
 * A data transfer object representing an analysis.
 * 
 * @author Dennis Roberts
 */
public class Analysis extends AbstractDto {

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
     * The e-mail address of the analysis integrator.
     */
    @JsonField(name = "integrator_email")
    private String integratorEmail;

    /**
     * The name of the analysis integrator.
     */
    @JsonField(name = "integrator_name")
    private String integratorName;

    /**
     * The date when the analysis was integrated.
     */
    @JsonField(name = "integration_date", optional = true, defaultValue = "")
    private Long integrationDate;

    /**
     * The date when the analysis was last edited.
     */
    @JsonField(name = "edited_date", optional = true, defaultValue = "")
    private Long editedDate;

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
     * The link to the analysis documentation.
     */
    @JsonField(name = "wiki_url")
    private String wikiUrl;

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
     * Pipeline eligibility information for this analysis.
     */
    @JsonField(name = "pipeline_eligibility")
    private AnalysisValidationDto pipelineEligibility;

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
     * @return the date when the analysis was last edited.
     */
    public Long getEditedDate() {
        return editedDate;
    }

    /**
     * @return the e-mail address of the analysis integrator.
     */
    public String getIntegratorEmail() {
        return integratorEmail;
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
     * @return the link to the analysis documentation.
     */
    public String getWikiUrl() {
        return wikiUrl;
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
     * @return the pipeline eligibility information for the analysis.
     */
    public AnalysisValidationDto getPipelineEligibility() {
        return pipelineEligibility;
    }

    public Analysis(AnalysisListing analysis) {
        initializeCommonFields(analysis);
        this.rating = new AnalysisRating(analysis);
        this.favorite = false;
    }

    /**
     * @param analysis the analysis represented by this DTO.
     * @param favorites the analysis group containing the user's favorites.
     * @param userRatings the user's analysis ratings and comment IDs.
     */
    public Analysis(AnalysisListing analysis, Set<AnalysisListing> favorites,
            Map<Long, UserRating> userRatings) {
        initializeCommonFields(analysis);
        this.rating = new AnalysisRating(analysis, userRatings);
        this.favorite = favorites.contains(analysis);
    }

    /**
     * Initializes the fields that are initialized in the same manner for both constructors.
     * 
     * @param analysis the listing for the analysis.
     */
    private void initializeCommonFields(AnalysisListing analysis) {
        this.id = analysis.getId();
        this.name = analysis.getName();
        this.description = StringUtils.defaultString(analysis.getDescription());
        this.integratorEmail = StringUtils.defaultString(analysis.getIntegratorEmail());
        this.integratorName = StringUtils.defaultString(analysis.getIntegratorName());
        this.integrationDate = dateAsLong(analysis.getIntegrationDate());
        this.editedDate = dateAsLong(analysis.getEditedDate());
        this.isPublic = analysis.isPublic();
        this.wikiUrl = StringUtils.defaultString(analysis.getWikiUrl());
        this.deleted = analysis.isDeleted();
        this.disabled = analysis.isDisabled();
        this.pipelineEligibility = new AnalysisValidationDto(analysis);
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
