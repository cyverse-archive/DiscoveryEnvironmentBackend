package org.iplantc.workflow.model;

import static org.junit.Assert.assertEquals;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.model.ContractType.
 * 
 * @author Dennis Roberts
 */
public class ContractTypeTest extends WorkflowElementTest<ContractType> {

    private static final String ID = "999";
    private static final String NAME = "someContractType";
    private static final String LABEL = "Some Contract Type";
    private static final String DESCRIPTION = "Description of some contract type.";

    /**
     * {@inheritDoc}
     */
    @Override
    protected ContractType createInstance() {
        return new ContractType(ID, NAME, LABEL, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ContractType createInstance(String id, String name, String label, String description) {
        return new ContractType(id, name, label, description);
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
     * Verifies that a contract type can accept a visit from a workflow marshaller.
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
        assertEquals(1, marshaller.getContractTypeVisits());
        assertEquals(1, marshaller.getContractTypeLeaves());
        assertEquals(0, marshaller.getValidatorVisits());
        assertEquals(0, marshaller.getValidatorLeaves());
        assertEquals(0, marshaller.getRuleVisits());
        assertEquals(0, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }
}
