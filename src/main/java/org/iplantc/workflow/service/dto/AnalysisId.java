package org.iplantc.workflow.service.dto;

/**
 * A data transfer object used to send an analysis ID.
 * 
 * @author Dennis Roberts
 */
public class AnalysisId extends AbstractDto {

    /**
     * The analysis identifier.
     */
    @JsonField(name = "analysis_id")
    protected String analysisId;

    /**
     * @param analysisId the analysis identifier.
     */
    public AnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
}
