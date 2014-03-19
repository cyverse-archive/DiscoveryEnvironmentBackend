package org.iplantc.workflow.model;

import java.util.ArrayList;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.model.RuleTest.
 * 
 * @author Dennis Roberts
 */
public class RuleTest extends WorkflowElementTest<Rule> {

    // The property values for this rule.
    private static final String ID = "812";
    private static final String NAME = "someRule";
    private static final String LABEL = "Some Rule";
    private static final String DESCRIPTION = "Description of some rule.";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Rule createInstance() {
        Rule rule = new Rule(ID, NAME, LABEL, DESCRIPTION);
        poplulateRule(rule);
        return rule;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Rule createInstance(String id, String name, String label, String description) {
        Rule rule = new Rule(id, name, label, description);
        poplulateRule(rule);
        return rule;
    }

    /**
     * Populates a new rule with a rule type and some arguments.
     * 
     * @param rule the rule to populate.
     */
    private void poplulateRule(Rule rule) {
        rule.setRuleType(new RuleType("9543", "blarg", "glarb", "blrfl"));
        rule.addArgument("foo");
        rule.addArgument("bar");
        rule.addArgument("baz");
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
     * Verifies that we can get the rule type.
     */
    @Test
    public void shouldSetRuleType() {
        assertEquals("9543", instance.getRuleType().getId());
    }

    /**
     * Verifies that we can get the arguments from the rule.
     */
    @Test
    public void shouldGetArguments() {
        assertEquals(Arrays.asList("foo", "bar", "baz"), instance.getArguments());
    }

    /**
     * Verifies that we can set the arguments for the rule.
     */
    @Test
    public void shouldSetArguments() {
        instance.setArguments(Arrays.asList("blarg", "glarb", "blrfl"));
        assertEquals(Arrays.asList("blarg", "glarb", "blrfl"), instance.getArguments());
    }

    /**
     * Verifies that a rule can accept a visit from a workflow marshaller.
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
        assertEquals(0, marshaller.getValidatorVisits());
        assertEquals(0, marshaller.getValidatorLeaves());
        assertEquals(1, marshaller.getRuleVisits());
        assertEquals(1, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }

    /**
     * Verifies that a rule without a rule type can accept a visit from a workflow marshaller.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void ruleWithoutTypeShouldAcceptMarshaller() throws Exception {
        MockWorkflowMarshaller marshaller = new MockWorkflowMarshaller();
        instance.setRuleType(null);
        instance.accept(marshaller);
        assertEquals(0, marshaller.getTemplateVisits());
        assertEquals(0, marshaller.getTemplateLeaves());
        assertEquals(0, marshaller.getPropertyGroupVisits());
        assertEquals(0, marshaller.getPropertyGroupLeaves());
        assertEquals(0, marshaller.getPropertyVisits());
        assertEquals(0, marshaller.getPropertyLeaves());
        assertEquals(0, marshaller.getPropertyTypeVisits());
        assertEquals(0, marshaller.getPropertyTypeLeaves());
        assertEquals(0, marshaller.getValidatorVisits());
        assertEquals(0, marshaller.getValidatorLeaves());
        assertEquals(1, marshaller.getRuleVisits());
        assertEquals(1, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }

    /**
     * Verifies rules with different rule types are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentRuleTypes() {
        Rule rule1 = createInstance();
        Rule rule2 = createInstance();
        rule2.setRuleType(new RuleType("1928", "foo", "bar", "baz"));
        assertFalse(rule1.equals(rule2));
    }

    /**
     * Verifies that rules with different arguments are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentRuleArguments() {
        Rule rule1 = createInstance();
        Rule rule2 = createInstance();
        rule2.addArgument("some as yet undiscovered argument");
        assertFalse(rule1.equals(rule2));
    }

    /**
     * Verifies that the rule type is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeRuleType() {
        Rule rule1 = createInstance();
        Rule rule2 = createInstance();
        assertTrue(rule1.hashCode() == rule2.hashCode());
        rule2.setRuleType(new RuleType("1928", "foo", "bar", "baz"));
        assertFalse(rule1.hashCode() == rule2.hashCode());
    }

    /**
     * Verifies that the rule arguments are included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeArguments() {
        Rule rule1 = createInstance();
        Rule rule2 = createInstance();
        assertTrue(rule1.hashCode() == rule2.hashCode());
        rule2.addArgument("some as yet undiscovered argument");
        assertFalse(rule1.hashCode() == rule2.hashCode());
    }

    /**
     * Verifies that an empty list can be validated without causing an error.
     */
    @Test
    public void emptyListShouldPassValidation() {
        createInstance().setArguments(new ArrayList<String>());
    }
}
