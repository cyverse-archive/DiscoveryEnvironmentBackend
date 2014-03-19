package org.iplantc.workflow;

/**
 * The base class used for all exceptions thrown by packages in this project.
 * 
 * @author Dennis Roberts
 */
public class WorkflowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a workflow exception that was caused by another exception or error.
     * 
     * @param cause the cause of the exception.
     */
    public WorkflowException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a workflow exception with the given error message.
     * 
     * @param msg the error message.
     */
    public WorkflowException(String msg) {
        super(msg);
        
    }

    /**
     * Creates a workflow exception with a custom error message and that was caused by another exception or error.
     * 
     * @param msg the error message.
     * @param cause the cause of the exception.
     */
    public WorkflowException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
