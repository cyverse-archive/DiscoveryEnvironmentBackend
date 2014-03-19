package org.iplantc.workflow.model;

import static org.junit.Assert.assertEquals;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.junit.Test;

public class RuleTypeTest extends WorkflowElementTest<RuleType> {

    private static String ID = "2001";
    private static String NAME = "someRuleType";
    private static String LABEL = "Some Rule Type";
    private static String DESCRIPTION = "Description of some rule type.";

    /**
     * Creates the RuleType instance to use for each of the tests.
     */
    @Override
    protected RuleType createInstance() {
        return new RuleType(ID, NAME, LABEL, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleType createInstance(String id, String name, String label, String description) {
        return new RuleType(id, name, label, description);
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
     * Verifies that a rule type can accept a visit from a marshaller.
     */
    @Test
    public void shouldAcceptMarshaller() {
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
        assertEquals(0, marshaller.getRuleVisits());
        assertEquals(0, marshaller.getRuleLeaves());
        assertEquals(1, marshaller.getRuleTypeVisits());
        assertEquals(1, marshaller.getRuleTypeLeaves());
    }
}
