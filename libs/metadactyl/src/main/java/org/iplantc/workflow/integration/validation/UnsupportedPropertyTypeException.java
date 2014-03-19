package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when a property type is not supported by the selected job type.
 * 
 * @author Dennis Roberts
 */
public class UnsupportedPropertyTypeException extends WorkflowException {

    /**
     * @param propertyType the selected property type.
     * @param jobType the selected job type.
     */
    public UnsupportedPropertyTypeException(String propertyType, String jobType) {
        super("property type, " + propertyType + ", is not supported for " + jobType + " jobs");
    }
}
