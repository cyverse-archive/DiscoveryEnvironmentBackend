package org.iplantc.workflow;

/**
 * Thrown when an unrecognized workflow element is specified.
 * 
 * @author Dennis Roberts
 */
public class UnknownWorkflowElementException extends WorkflowException {

    /**
     * @param elementName the name of the workflow element.
     * @param fieldName the name of the field used in the search.
     * @param fieldValue the field value used in the search.
     */
    public UnknownWorkflowElementException(String elementName, String fieldName, String fieldValue) {
        super("no " + elementName + " found with value of " + fieldValue + " in the " + fieldName + " field");
    }
}
