package org.iplantc.workflow.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.model.Validator.
 * 
 * @author Dennis Roberts
 */
public class ValidatorTest extends WorkflowElementTest<Validator> {

    // The values for the validator members.
    public static final String ID = "2112";
    public static final String NAME = "someWorkflowValidator";
    public static final String LABEL = "Some Workflow Validator";
    public static final String DESCRIPTION = "Description of some workflow validator.";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Validator createInstance() {
        Validator validator = new Validator(ID, NAME, LABEL, DESCRIPTION);
        populateValidator(validator);
        return validator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Validator createInstance(String id, String name, String label, String description) {
        Validator validator = new Validator(id, name, label, description);
        populateValidator(validator);
        return validator;
    }

    /**
     * Populates a validator with some rules.
     * 
     * @param validator the validator to populate.
     */
    private void populateValidator(Validator validator) {
        Rule rule1 = new Rule("1", "a", "a", "a");
        Rule rule2 = new Rule("2", "b", "b", "b");
        rule2.setRuleType(new RuleType("3", "c", "c", "c"));
        validator.addRule(rule1);
        validator.addRule(rule2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementLabel() {
        return LABEL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementDescription() {
        return DESCRIPTION;
    }

    /**
     * Verifies that properties are required by default.
     */
    @Test
    public void propertyShouldBeRequiredByDefault() {
        assertTrue(instance.isRequired());
    }

    /**
     * Verifies that we can set the required attribute of the class.
     */
    @Test
    public void shouldSetRequired() {
        instance.setRequired(false);
        assertFalse(instance.isRequired());
    }

    /**
     * Verifies that we can get the list of rules from the validator.
     */
    @Test
    public void shouldGetRules() {
        assertEquals(2, instance.getRules().size());
        assertEquals("1", instance.getRules().get(0).getId());
        assertEquals("2", instance.getRules().get(1).getId());
    }

    /**
     * Verifies that we can set the list of rules.
     */
    @Test
    public void shouldSetRules() {
        instance.setRules(Arrays.asList(new Rule(), new Rule(), new Rule(), new Rule()));
        assertEquals(4, instance.getRules().size());
    }

    /**
     * Verifies that a validator can accept a visit from a workflow marshaller.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void shouldAcceptMarshaller() throws Exception {
        MockWorkflowMarshaller marshaller = new MockWorkflowMarshaller();
        instance.accept(marshaller);
        assertEquals(0, marshaller.getTemplateVisits());
        assertEquals(0, marshaller.getTemplateLeaves());
        assertEquals(0, marshaller.getPropertyGroupVisits());
        assertEquals(0, marshaller.getPropertyGroupLeaves());
        assertEquals(0, marshaller.getPropertyVisits());
        assertEquals(0, marshaller.getPropertyLeaves());
        assertEquals(0, marshaller.getPropertyTypeVisits());
        assertEquals(0, marshaller.getPropertyTypeLeaves());
        assertEquals(1, marshaller.getValidatorVisits());
        assertEquals(1, marshaller.getValidatorLeaves());
        assertEquals(2, marshaller.getRuleVisits());
        assertEquals(2, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }

    /**
     * Verifies that two validators with different required flags are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentRequiredFlags() {
        Validator validator1 = createInstance();
        Validator validator2 = createInstance();
        assertTrue(validator1.equals(validator2));
        validator2.setRequired(false);
        assertFalse(validator1.equals(validator2));
    }

    /**
     * Verifies that two validators with different rule sets are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentRuleSets() {
        Validator validator1 = createInstance();
        Validator validator2 = createInstance();
        assertTrue(validator1.equals(validator2));
        validator2.addRule(new Rule("4973", "blarg", "glarb", "blurfl"));
        assertFalse(validator1.equals(validator2));
    }

    /**
     * Verifies that the required flag is included in the calculation of the hash code.
     */
    @Test
    public void hashCodeShouldIncludeRequiredFlag() {
        Validator validator1 = createInstance();
        Validator validator2 = createInstance();
        assertTrue(validator1.hashCode() == validator2.hashCode());
        validator2.setRequired(false);
        assertFalse(validator1.hashCode() == validator2.hashCode());
    }

    /**
     * Verifies that the rule set is included in the calculation of the hash code.
     */
    @Test
    public void hashCodeShouldIncludeRuleSet() {
        Validator validator1 = createInstance();
        Validator validator2 = createInstance();
        assertTrue(validator1.hashCode() == validator2.hashCode());
        validator2.addRule(new Rule("4973", "blarg", "glarb", "blurfl"));
        assertFalse(validator1.hashCode() == validator2.hashCode());
    }
}
