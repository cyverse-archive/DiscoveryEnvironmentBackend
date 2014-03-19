package org.iplantc.workflow;

/**
 * Thrown when an unknown data source name is specified in a workflow.
 * 
 * @author Dennis Roberts
 */
public class UnknownDataSourceException extends UnknownWorkflowElementException {

    /**
     * @param fieldName the name of the field used in the search.
     * @param fieldValue the field value used in the search.
     */
    public UnknownDataSourceException(String fieldName, String fieldValue) {
        super("DataSource", fieldName, fieldValue);
    }
}
