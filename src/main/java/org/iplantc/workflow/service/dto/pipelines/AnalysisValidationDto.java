package org.iplantc.workflow.service.dto.pipelines;

import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;
import org.iplantc.workflow.service.util.PipelineAnalysisValidator;

/**
 * Used to communicate information about analysis validation to a caller.
 * 
 * @author Dennis Roberts
 */
public class AnalysisValidationDto extends AbstractDto {

    /**
     * True if the analysis can be used in a pipeline.
     */
    @JsonField(name = "is_valid")
    private boolean valid;

    /**
     * The reason the analysis can't be used in a pipeline.
     */
    @JsonField(name = "reason")
    private String reason;

    /**
     * @return true if the analysis can be used in a pipeline.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return the reason the analysis can't be used in a pipeline.
     */
    public String getReason() {
        return reason;
    }

    /**
     * @param analysisId the analysis identifier.
     * @param daoFactory used to obtain data access objects.
     */
    public AnalysisValidationDto(String analysisId, DaoFactory daoFactory) {
        try {
            PipelineAnalysisValidator.validateAnalysis(analysisId, daoFactory);
            valid = true;
            reason = "";
        }
        catch (WorkflowException e) {
            valid = false;
            reason = e.getMessage();
        }
    }

    /**
     * @param analysis the analysis.
     */
    public AnalysisValidationDto(TransformationActivity analysis) {
        try {
            PipelineAnalysisValidator.validateAnalysis(analysis);
            valid = true;
            reason = "";
        }
        catch (WorkflowException e) {
            valid = false;
            reason = e.getMessage();
        }
    }

    /**
     * @param analysis the analysis listing.
     */
    public AnalysisValidationDto(AnalysisListing analysis) {
        try {
            PipelineAnalysisValidator.validateAnalysis(analysis);
            valid = true;
            reason = "";
        }
        catch (WorkflowException e) {
            valid = false;
            reason = e.getMessage();
        }
    }
}
