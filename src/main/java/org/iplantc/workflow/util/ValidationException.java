package org.iplantc.workflow.util;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when a field validation fails.
 * 
 * @author Dennis Roberts
 */
public class ValidationException extends WorkflowException {

    /**
     * @param msg the detail message.
     */
    public ValidationException(String msg) {
        super(msg);
    }
}
