package org.iplantc.workflow.service.util;

import org.apache.log4j.Logger;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.persistence.dto.listing.JobType;
import org.iplantc.persistence.dto.listing.PipelineCandidate;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * Used to validate analyses that may be selected in a pipeline.
 * 
 * @author Dennis Roberts
 */
public class PipelineAnalysisValidator {
    
    private static final Logger LOG = Logger.getLogger(PipelineAnalysisValidator.class);

    /**
     * Prevent instantiation.
     */
    private PipelineAnalysisValidator() {}

    /**
     * Validates analyses that may be selected in a pipeline.
     * 
     * @param analysis the analysis to validate.
     */
    public static void validateAnalysis(String analysisId, DaoFactory daoFactory) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis, " + analysisId + ", not found");
        }
        validateAnalysis(analysis);
    }

    /**
     * Validates analyses that may be selected in a pipeline.
     * 
     * @param analysis the listing for the analysis to validate.
     */
    public static void validateAnalysis(PipelineCandidate analysis) {
        if (analysis.getStepCount() < 1) {
            throw new WorkflowException("analysis, " + analysis.getId() + ", has too few steps for a pipeline");
        }
        if (analysis.getStepCount() > 1) {
            throw new WorkflowException("analysis, " + analysis.getId() + ", has too many steps for a pipeline");
        }
        if (analysis.getOverallJobType() != JobType.EXECUTABLE) {
            String msg = "job type, " + analysis.getOverallJobType() + ", can't currently be included in a pipeline";
            throw new WorkflowException(msg);
        }
    }
}
