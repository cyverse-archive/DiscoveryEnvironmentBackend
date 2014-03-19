package org.iplantc.workflow;

/**
 * Indicates that a required field was missing in an incoming JSON object.
 * 
 * @author Dennis Roberts
 */
public class MissingRequiredFieldException extends WorkflowException {

    /**
     * @param fieldName the name of the missing field.
     */
    public MissingRequiredFieldException(String fieldName) {
        super("Missing required incoming field: " + fieldName);
    }
}
