package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when more than one output data object is associated with either the standard output or standard error output
 * stream.
 *
 * @author Dennis Roberts
 */
public class TooManyOutputRedirectionsException extends WorkflowException {

    /**
     * @param templateName the name of the template containing the error.
     * @param streamName the name of the output stream to which too many output data objects are mapped.
     */
    public TooManyOutputRedirectionsException(String templateName, String streamName) {
        super("more than one job output is associated with the " + streamName + " stream for the template, "
                + templateName);

    }
}
