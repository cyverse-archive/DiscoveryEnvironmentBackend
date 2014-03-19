package org.iplantc.workflow;

/**
 * Thrown when a user attempts to edit a public analysis.
 *
 * @author Dennis Roberts
 */
public class AnalysisPublicException extends WorkflowException {

    /**
     * @param analysisId the analysis identifier.
     */
    public AnalysisPublicException(String analysisId) {
        super("analysis " + analysisId + " is public and may not be edited");
    }
}
