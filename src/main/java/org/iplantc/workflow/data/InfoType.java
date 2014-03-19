package org.iplantc.workflow.data;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.iplantc.workflow.model.WorkflowElement;

/**
 * Represents the type of information contained in a data object. Some common examples are tree data and trait data.
 *
 * @author Dennis Roberts
 */
public class InfoType extends WorkflowElement {

    /**
     * True if the information type has been deprecated.
     */
    private boolean deprecated;

    /**
     * Specifies the relative display order of the information type.
     */
    private int displayOrder;

    /**
     * @param deprecated the new deprecation flag.
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * @return the deprecation flag.
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @param displayOrder the new display order.
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * @return the display order.
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Creates a new empty information type object.
     */
    public InfoType() {
        super();
    }

    /**
     * Creates a new information type object with the given ID, name, label, and description.
     *
     * @param id the information type identifier.
     * @param name the information type name.
     * @param label the information type label.
     * @param description the information type descripiton.
     */
    public InfoType(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof InfoType) {
            equals = super.equals(o);
        }
        return equals;
    }
}
