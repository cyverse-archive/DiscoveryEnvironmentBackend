package org.iplantc.workflow;

/**
 * Thrown when an unknown update mode name is specified in a service call.
 * 
 * @author Dennis Roberts
 */
public class UnknownUpdateModeException extends UnknownWorkflowElementException {

    /**
     * @param updateModeName the name of the update mode.
     */
    public UnknownUpdateModeException(String updateModeName) {
        super ("UpdateMode", "name", updateModeName);
    }
}
