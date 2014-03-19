package org.iplantc.workflow;

/**
 * Thrown when a user selects an unknown job type.
 * 
 * @author Dennis Roberts
 */
public class UnknownToolTypeException extends UnknownWorkflowElementException {

    /**
     * @param fieldName the name of the field that we searched for.
     * @param fieldValue the value of the field that we searched for.
     */
    public UnknownToolTypeException(String fieldName, String fieldValue) {
        super("ToolType", fieldName, fieldValue);
    }
}
