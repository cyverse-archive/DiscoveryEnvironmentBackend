package org.iplantc.persistence.dto.listing;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Contains the information required to represent an analysis in the analysis listing services.
 * 
 * @author Dennis Roberts
 */
@NamedQueries({
        @NamedQuery(name = "AnalysisListing.findById", query = "from AnalysisListing where hid = :id"),
        @NamedQuery(name = "AnalysisListing.findByExternalId", query = "from AnalysisListing where id = :id")
})
@Entity
@Table(name = "analysis_listing")
public class AnalysisListing implements Serializable, PipelineCandidate {

    /**
     * The internal analysis identifier.
     */
    @Id
    private long hid;

    /**
     * The external analysis identifier.
     */
    @Column(name = "id")
    private String id;

    /**
     * The analysis name.
     */
    @Column(name = "name")
    private String name;

    /**
     * The analysis description;
     */
    @Column(name = "description")
    private String description;

    /**
     * The name of the person who integrated the analysis.
     */
    @Column(name = "integrator_name")
    private String integratorName;

    /**
     * The e-mail address of the person who integrated the analysis.
     */
    @Column(name = "integrator_email")
    private String integratorEmail;

    /**
     * The date when the analysis was integrated.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "integration_date")
    private Date integrationDate;

    /**
     * The date when the analysis was last edited.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "edited_date")
    private Date editedDate;

    /**
     * The link to the analysis documentation page in the wiki.
     */
    @Column(name = "wikiurl")
    private String wikiUrl;

    /**
     * The average rating for the analysis.
     */
    @Column(name = "average_rating")
    private double averageRating;

    /**
     * True if the analysis is in a public analysis group.
     */
    @Column(name = "is_public")
    private boolean isPublic;

    /**
     * The number of steps in the analysis.
     */
    @Column(name = "step_count")
    private long stepCount;

    /**
     * True if the analysis has been marked as deleted.
     */
    @Column(name = "deleted")
    private boolean deleted;

    /**
     * True if the analysis has been marked as disabled.
     */
    @Column(name = "disabled")
    private boolean disabled;

    /**
     * The list of deployed components used by the analysis.
     */
    @OneToMany(mappedBy = "analysisId")
    @OrderColumn(name = "execution_order")
    private List<DeployedComponentListing> deployedComponents;

    /**
     * The overall job type for the analysis.
     */
    @Column(name = "overall_job_type")
    private String overallJobTypeName;

    /**
     * @return the internal analysis identifier
     */
    public long getHid() {
        return hid;
    }

    /**
     * @return the analysis description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the external analysis identifier
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the date when the analysis was integrated
     */
    public Date getIntegrationDate() {
        return integrationDate;
    }

    /**
     * @return the date when the analysis was last edited.
     */
    public Date getEditedDate() {
        return editedDate;
    }

    /**
     * @return the e-mail address of the person who integrated the analysis
     */
    public String getIntegratorEmail() {
        return integratorEmail;
    }

    /**
     * @return the name of the person who integrated the analysis
     */
    public String getIntegratorName() {
        return integratorName;
    }

    /**
     * @return true if the analysis is in a public analysis group
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @return the name of the analysis
     */
    public String getName() {
        return name;
    }

    /**
     * @return the number of steps in the analysis
     */
    @Override
    public long getStepCount() {
        return stepCount;
    }

    /**
     * @return the link to the analysis documentation page in the wiki
     */
    public String getWikiUrl() {
        return wikiUrl;
    }

    /**
     * @return the list of deployed components used by the analysis
     */
    public List<DeployedComponentListing> getDeployedComponents() {
        return deployedComponents;
    }

    /**
     * @return the average rating for this analysis.
     */
    public double getAverageRating() {
        return averageRating;
    }

    /**
     * @return true if this analysis has been deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @return true if this analysis has been disabled by an administrator.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @return the name of the overall job type.
     */
    public String getOverallJobTypeName() {
        return overallJobTypeName;
    }

    /**
     * @param overallJobTypeName the name of the overall job type.
     */
    public void setOverallJobTypeName(String overallJobTypeName) {
        this.overallJobTypeName = overallJobTypeName;
    }

    /**
     * @return the overall job type for this analysis.
     */
    @Override
    public JobType getOverallJobType() {
        return JobType.fromString(overallJobTypeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AnalysisListing other = (AnalysisListing) obj;
        if (this.hid != other.hid) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (int) (this.hid ^ (this.hid >>> 32));
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AnalysisListing{" + "hid=" + hid + ", id=" + id + ", name=" + name + ", description=" + description +
                ", integratorName=" + integratorName + ", integratorEmail=" + integratorEmail + ", integrationDate=" +
                integrationDate + ", editedDate=" + editedDate + ", wikiUrl=" + wikiUrl + ", averageRating=" +
                averageRating + ", isPublic=" + isPublic + ", stepCount=" + stepCount + ", deleted=" + deleted +
                ", disabled=" + disabled + ", deployedComponents=" + deployedComponents + ", overallJobTypeName=" +
                overallJobTypeName + '}';
    }
}
