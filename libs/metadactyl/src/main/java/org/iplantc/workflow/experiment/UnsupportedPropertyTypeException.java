package org.iplantc.workflow.experiment;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when a property type that is not supported is encountered.
 * 
 * @author Dennis Roberts
 */
public class UnsupportedPropertyTypeException extends WorkflowException {

    /**
     * @param propertyTypeName the name of the unsupported property type.
     */
    public UnsupportedPropertyTypeException(String propertyTypeName) {
        super("unsupported property type, " + propertyTypeName + ", encountered");
    }
}
