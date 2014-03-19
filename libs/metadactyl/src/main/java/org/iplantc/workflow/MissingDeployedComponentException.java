package org.iplantc.workflow;

/**
 * Thrown when someone tries to perform an action that requires a deployed component with an app containing a template
 * that doesn't have a deployed component associated with it.
 */
public class MissingDeployedComponentException extends WorkflowException {

    /**
     * @param templateId the template identifier.
     */
    public MissingDeployedComponentException(String templateId) {
        super(templateId + " has no executable associated with it");
    }
}
