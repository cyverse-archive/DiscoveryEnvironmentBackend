package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;

/**
 * Represents a validation rule.
 *
 * @author Dennis Roberts
 */
public class Rule extends WorkflowElement {

    /**
     * The validation rule type.
     */
    private RuleType ruleType;

    /**
     * The list of arguments for this rule.
     */
    private List<String> arguments = new LinkedList<String>();

    /**
     * Sets the rule type.
     *
     * @param ruleType the new rule type.
     */
    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    /**
     * Gets the rule type.
     *
     * @return the rule type.
     */
    public RuleType getRuleType() {
        return ruleType;
    }

    /**
     * Adds an argument to the list of arguments.
     *
     * @param argument the argument to add.
     */
    public void addArgument(String argument) {
        arguments.add(argument);
    }

    /**
     * Sets the list of arguments. Hibernate requires a setter.
     *
     * @param arguments the new list of arguments.
     */
    public void setArguments(List<String> arguments) {
        this.arguments = new LinkedList<String>(arguments);
    }

    /**
     * Gets the list of arguments for this rule.
     *
     * @return an unmodifiable copy of the list of arguments.
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Creates a new empty rule.
     */
    public Rule() {
        super();
    }

    /**
     * Creates a new rule with the given ID, name, label and description.
     *
     * @param id the rule identifier.
     * @param name the rule name.
     * @param label the rule label.
     * @param description the rule description.
     */
    public Rule(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);
       /* if (ruleType != null) {
            ruleType.accept(marshaller);
        }*/
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof Rule) {
            Rule other = (Rule) otherObject;
            if (!super.equals(other)) {
                return false;
            }
            if (!ObjectUtils.equals(ruleType, other.getRuleType())) {
                return false;
            }
            if (!ObjectUtils.equals(arguments, other.getArguments())) {
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
        hashCode += ObjectUtils.hashCode(ruleType);
        hashCode += ObjectUtils.hashCode(arguments);
        return hashCode;
    }
}
