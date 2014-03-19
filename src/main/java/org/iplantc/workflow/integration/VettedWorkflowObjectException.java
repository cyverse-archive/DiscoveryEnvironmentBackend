package org.iplantc.workflow.integration;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when an object is imported to replace an already vetted object.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class VettedWorkflowObjectException extends WorkflowException {
    public VettedWorkflowObjectException(String msg) {
        super(msg);
    }
}
