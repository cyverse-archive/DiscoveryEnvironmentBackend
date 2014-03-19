package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;

/**
 * Represents a sub-category for property validation rules.
 *
 * @author Dennis Roberts
 */
public class RuleSubtype extends WorkflowElement {

    /**
     * Creates a new empty rule sub-type.
     */
    public RuleSubtype() {
        super();
    }

    /**
     * @param id the rule sub-type identifier.
     * @param name the rule sub-type name.
     * @param label the rule sub-type label.
     * @param description the rule sub-type description.
     */
    public RuleSubtype(String id, String name, String label, String description) {
        super(id, name, label, description);
        validateFieldLength(this.getClass(), "id", id, 40);
        validateFieldLength(this.getClass(), "name", name, 40);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        // Rule sub-types are not currently marshalled.
    }
}
