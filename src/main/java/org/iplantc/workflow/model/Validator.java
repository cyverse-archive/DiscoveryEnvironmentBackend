package org.iplantc.workflow.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;

/**
 * Used to validate property values that have been entered by the user.
 *
 * @author Dennis Roberts
 */
public class Validator extends WorkflowElement {

    /**
     * True if the property being validated is required.
     */
    private Boolean required = true;

    /**
     * The list of rules for this validator.
     */
    private List<Rule> rules = new LinkedList<Rule>();

    /**
     * Sets the required flag for this validator.
     *
     * @param required true if the property must be populated.
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * Indicates whether or not the property being validated is required.
     *
     * @return true if the property being validated must be populated.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Adds a rule to the list of rules for this validator.
     *
     * @param rule the rule to add.
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Sets the list of rules for this validator. Hibernate requires a setter.
     *
     * @param rules the new list of rules.
     */
    public void setRules(List<Rule> rules) {
        this.rules = new LinkedList<Rule>(rules);
    }

    /**
     * Returns the list of rules.
     *
     * @return an unmodifiable copy of the list of rules.
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Creates an empty validator.
     */
    public Validator() {
        super();
    }

    /**
     * Creates a validator with the given ID, name, label, description.
     *
     * @param id the validator identifier.
     * @param name the validator name.
     * @param label the validator label.
     * @param description the validator description.
     */
    public Validator(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);
        for (Rule rule : rules) {
            rule.accept(marshaller);
        }
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof Validator) {
            Validator other = (Validator) otherObject;
            if (!super.equals(other)) {
                return false;
            }
            if (required != other.isRequired()) {
                return false;
            }
            if (!ObjectUtils.equals(rules, other.getRules())) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode += required.hashCode();
        hashCode += ObjectUtils.hashCode(rules);
        return hashCode;
    }
}
