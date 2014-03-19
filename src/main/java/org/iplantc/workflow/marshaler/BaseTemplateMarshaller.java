package org.iplantc.workflow.marshaler;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.ContractType;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.PropertyType;
import org.iplantc.workflow.model.Rule;
import org.iplantc.workflow.model.RuleType;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.model.Validator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marshalls a workflow.
 *
 * @author Dennis Roberts
 */
public interface BaseTemplateMarshaller {

    /**
     * Gets the marshalled workflow.
     *
     * @throws WorkflowException if an error occurs.
     */
    public String getMarshalledWorkflow() throws WorkflowException;

    /**
     * Returns the cumulative JSON object after marshalling.
     *
     * @return the cumulative JSON object.
     * @throws WorkflowException if nothing has been marshalled yet.
     */
    public JSONObject getCumulativeJson() throws WorkflowException;

    /**
     * Begins marshalling a template.
     *
     * @param template the template to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(Template template) throws WorkflowException;

    /**
     * Called to indicate that we've finished marshalling a template.
     *
     * @param template the template being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(Template template) throws WorkflowException;

    /**
     * Begins marshalling a property group.
     *
     * @param propertyGroup the property group to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(PropertyGroup propertyGroup) throws WorkflowException;

    /**
     * Called to indicate that we've finished marshalling a property group.
     *
     * @param propertyGroup the property group being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(PropertyGroup propertyGroup) throws WorkflowException;

    /**
     * Begins marshalling a property.
     *
     * @param property the property to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(Property property) throws WorkflowException;

    /**
     * Called to indicate that we've finished marshalling a property.
     *
     * @param property the property being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(Property property) throws WorkflowException;

    /**
     * Begins marshalling a contract type.
     *
     * @param contractType the contract type to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(ContractType contractType) throws WorkflowException;

    /**
     * Called to indicate when we've finished marshalling a contract type.
     *
     * @param contractType the contract type being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(ContractType contractType) throws WorkflowException;

    /**
     * Begins marshalling a property type.
     *
     * @param propertyType the property type to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(PropertyType propertyType) throws WorkflowException;

    /**
     * Called to indicate that we've finished marshalling a property type.
     *
     * @param propertyType the property type being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(PropertyType propertyType) throws WorkflowException;

    /**
     * Begins marshalling a validator.
     *
     * @param validator the validator to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(Validator validator) throws WorkflowException;

    /**
     * Called to indicate that we've finished marshalling a validator.
     *
     * @param validator the validator being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(Validator validator) throws WorkflowException;

    /**
     * Begins marshalling a rule.
     *
     * @param rule the rule to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(Rule rule) throws WorkflowException;

    /**
     * Called to indicate that we've finished marshalling a rule.
     *
     * @param rule the rule being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(Rule rule) throws WorkflowException;

    /**
     * Begins marshalling a rule type.
     *
     * @param ruleType the rule type to marshall.
     * @throws WorkflowException if an error occurs.
     */
    public void visit(RuleType ruleType) throws WorkflowException;

    public void visitInputs(List<DataObject> input) throws JSONException;

    public void leaveInputs(List<DataObject> input) throws JSONException;

    /**
     * Called to indicate that we've finished marshalling a rule type.
     *
     * @param ruleType the rule type being marshalled.
     * @throws WorkflowException if an error occurs.
     */
    public void leave(RuleType ruleType) throws WorkflowException;
}
