package org.iplantc.workflow.model;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;

/**
 * Represents a type of dependency between two property groups.
 *
 * @author Dennis Roberts
 */
public class ContractType extends WorkflowElement {

    /**
     * Creates a new empty contract type.
     */
    public ContractType() {
        super();
    }

    /**
     * Creates a new contract type with the given ID, name, label and description.
     *
     * @param id the contract type identifier.
     * @param name the contract type name.
     * @param label the contract type label.
     * @param description the contract type description.
     */
    public ContractType(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof ContractType) {
            return super.equals(otherObject);
        }
        return false;
    }
}
