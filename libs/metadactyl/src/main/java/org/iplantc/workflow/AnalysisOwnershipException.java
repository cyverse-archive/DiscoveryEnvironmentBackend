package org.iplantc.workflow;

/**
 * Thrown when a user attempts to edit an analysis that she doesn't own.
 *
 * @author Dennis Roberts
 */
public class AnalysisOwnershipException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    /**
     * @param username the name of the user who tried to edit the analysis.
     * @param analysisId the analysis identifier.
     */
    public AnalysisOwnershipException(String username, String analysisId) {
        super(username + " does not own analysis " + analysisId);
    }
}
