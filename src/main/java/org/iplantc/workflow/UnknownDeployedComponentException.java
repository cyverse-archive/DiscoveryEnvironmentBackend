package org.iplantc.workflow;

/**
 * Thrown when an unknown deployed component is specified.
 */
public class UnknownDeployedComponentException extends UnknownWorkflowElementException {

    /**
     * @param id the ID of the deployed component that couldn't be found.
     */
    public UnknownDeployedComponentException(String id) {
        super("deployed component", "id", id);
    }
}
