package org.iplantc.workflow;

/**
 * Thrown when a metadata element can't be found.
 *
 * @author Dennis Roberts
 */
public abstract class ElementNotFoundException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    /**
     * @param elementType a brief description of the type of element that couldn't be found.
     * @param elementId the element identifier.
     */
    public ElementNotFoundException(String elementType, String elementId) {
        super(elementType + " " + elementId + " not found");
    }
}
