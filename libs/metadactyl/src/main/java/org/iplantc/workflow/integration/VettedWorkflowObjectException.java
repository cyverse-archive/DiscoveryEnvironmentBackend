package org.iplantc.workflow.integration;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when an object is imported to replace an already vetted object.
 * 
 * @author Kris Healy &lt;healyk@iplantcollaborative.org&gt;
 */
public class VettedWorkflowObjectException extends WorkflowException {
    public VettedWorkflowObjectException(String msg) {
        super(msg);
    }
}
