package org.iplantc.workflow.model;

import static org.junit.Assert.assertEquals;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.model.PropertyType.
 * 
 * @author Dennis Roberts
 */
public class PropertyTypeTest extends WorkflowElementTest<PropertyType> {

    // The values to use for the property type members.
    private static final String ID = "123";
    private static final String NAME = "somePropertyType";
    private static final String LABEL = "Some Property Type";
    private static final String DESCRIPTION = "The description of some property type";

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertyType createInstance() {
        return new PropertyType(ID, NAME, LABEL, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertyType createInstance(String id, String name, String label, String description) {
        return new PropertyType(id, name, label, description);
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
     * Verifies that a workflow marshaller can visit a property type.
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
        assertEquals(1, marshaller.getPropertyTypeVisits());
        assertEquals(1, marshaller.getPropertyTypeLeaves());
        assertEquals(0, marshaller.getValidatorVisits());
        assertEquals(0, marshaller.getValidatorLeaves());
        assertEquals(0, marshaller.getRuleVisits());
        assertEquals(0, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }
}
