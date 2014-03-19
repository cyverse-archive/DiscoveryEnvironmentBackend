package org.iplantc.workflow;

/**
 * Thrown when a user attempts to edit an analysis that does not have exactly one step.
 *
 * @author Dennis Roberts
 */
public class AnalysisStepCountException extends WorkflowException {

    /**
     * @param analysisId the analysis identifier.
     * @param stepCount the number of steps.
     */
    public AnalysisStepCountException(String analysisId, long stepCount) {
        super("only analyses with exactly one step may be edited; analysis " + analysisId + " has " + stepCount);
    }
}
