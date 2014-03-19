package org.iplantc.workflow.mock;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
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
 * A mock workflow marshaller to be used for testing the accept() methods in each of the workflow elements.
 *
 * @author Dennis Roberts
 */
public class MockWorkflowMarshaller implements BaseTemplateMarshaller {

    /**
     * The number of visits to a template.
     */
    private int templateVisits = 0;

    /**
     * The number of times we've left a template.
     */
    private int templateLeaves = 0;

    /**
     * The number of visits to a property group.
     */
    private int propertyGroupVisits = 0;

    /**
     * The number of times we've left a property group.
     */
    private int propertyGroupLeaves = 0;

    /**
     * The number of visits to a property.
     */
    private int propertyVisits = 0;

    /**
     * The number of times we've left a property.
     */
    private int propertyLeaves = 0;

    /**
     * The number of visits to a property type.
     */
    private int propertyTypeVisits = 0;

    /**
     * The number of times we've left a property type.
     */
    private int propertyTypeLeaves = 0;

    /**
     * The number of visits to a contract type.
     */
    private int contractTypeVisits = 0;

    /**
     * The number of times we've left a contract type.
     */
    private int contractTypeLeaves = 0;

    /**
     * The number of visits to a validator
     */
    private int validatorVisits = 0;

    /**
     * The number of times we've left a validator.
     */
    private int validatorLeaves = 0;

    /**
     * The number of visits to a rule.
     */
    private int ruleVisits = 0;

    /**
     * The number of times we've left a rule.
     */
    private int ruleLeaves = 0;

    /**
     * The number of visits to a rule type.
     */
    private int ruleTypeVisits = 0;

    /**
     * The number of times we've left a rule type.
     */
    private int ruleTypeLeaves = 0;

    /**
     * Gets the number of times this marshaller has visited a template.
     *
     * @return the number of template visits.
     */
    public int getTemplateVisits() {
        return templateVisits;
    }

    /**
     * Gets the number of times this marshaller has left a template.
     *
     * @return the number of template leaves.
     */
    public int getTemplateLeaves() {
        return templateLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a property group.
     *
     * @return the number of property group visits.
     */
    public int getPropertyGroupVisits() {
        return propertyGroupVisits;
    }

    /**
     * Gets the number of times this marshaller has left a property group.
     *
     * @return the number of property group leaves.
     */
    public int getPropertyGroupLeaves() {
        return propertyGroupLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a property.
     *
     * @return the number of property visits.
     */
    public int getPropertyVisits() {
        return propertyVisits;
    }

    /**
     * Gets the number of times this marshaller has left a property.
     *
     * @return the number of property leaves.
     */
    public int getPropertyLeaves() {
        return propertyLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a property type.
     *
     * @return the number of property type visits.
     */
    public int getPropertyTypeVisits() {
        return propertyTypeVisits;
    }

    /**
     * Gets the number of times this marshaller has left a property type.
     *
     * @return the number of property type leaves.
     */
    public int getPropertyTypeLeaves() {
        return propertyTypeLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a contract type.
     *
     * @return the number of contract type visits.
     */
    public int getContractTypeVisits() {
        return contractTypeVisits;
    }

    /**
     * Gets the number of times this marshaller has left a contract type.
     *
     * @return the number of contract type leaves.
     */
    public int getContractTypeLeaves() {
        return contractTypeLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a validator.
     *
     * @return the number of validator visits.
     */
    public int getValidatorVisits() {
        return validatorVisits;
    }

    /**
     * Gets the number of times this marshaller has left a validator.
     *
     * @return the number of validator leaves.
     */
    public int getValidatorLeaves() {
        return validatorLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a rule.
     *
     * @return the number of rule visits.
     */
    public int getRuleVisits() {
        return ruleVisits;
    }

    /**
     * Gets the number of times this marshaller has left a rule.
     *
     * @return the number of rule leaves.
     */
    public int getRuleLeaves() {
        return ruleLeaves;
    }

    /**
     * Gets the number of times this marshaller has visited a rule type.
     *
     * @return the number of rule type visits.
     */
    public int getRuleTypeVisits() {
        return ruleTypeVisits;
    }

    /**
     * Gets the number of times this marshaller has left a rule type.
     *
     * @return the number of rule type leaves.
     */
    public int getRuleTypeLeaves() {
        return ruleTypeLeaves;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMarshalledWorkflow() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getCumulativeJson() throws WorkflowException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(Template template) {
        templateVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(Template template) {
        templateLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(PropertyGroup propertyGroup) {
        propertyGroupVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(PropertyGroup propertyGroup) {
        propertyGroupLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(Property property) {
        propertyVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(Property property) {
        propertyLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(PropertyType propertyType) {
        propertyTypeVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(PropertyType propertyType) {
        propertyTypeLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(ContractType contractType) throws WorkflowException {
        contractTypeVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(ContractType contractType) throws WorkflowException {
        contractTypeLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(Validator validator) throws WorkflowException {
        validatorVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(Validator validator) throws WorkflowException {
        validatorLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(Rule rule) throws WorkflowException {
        ruleVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(Rule rule) throws WorkflowException {
        ruleLeaves++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(RuleType ruleType) throws WorkflowException {
        ruleTypeVisits++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave(RuleType ruleType) throws WorkflowException {
        ruleTypeLeaves++;
    }

    public void leaveInputs(List<DataObject> input) throws JSONException {

    }

    public void visitInputs(List<DataObject> input) throws JSONException {

    }
}
