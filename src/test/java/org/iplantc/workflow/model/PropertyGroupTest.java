package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.UnitTestUtils.longString;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.iplantc.workflow.util.FieldLengthValidationException;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.model.PropertyGroupTest.
 * 
 * @author Dennis Roberts
 */
public class PropertyGroupTest extends WorkflowElementTest<PropertyGroup> {

    // The properties to use for each of the tests.
    private static final String ID = "42";
    private static final String NAME = "somePropertyGroup";
    private static final String LABEL = "Some Property Group";
    private static final String DESCRIPTION = "Description of some property group.";

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertyGroup createInstance() {
        return new PropertyGroup(ID, NAME, LABEL, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertyGroup createInstance(String id, String name, String label, String description) {
        return new PropertyGroup(id, name, label, description);
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
     * Verifies that we can add properties.
     */
    @Test
    public void shouldAddProperties() {
        instance.addProperty(new Property("1", "a", "a", "a"));
        instance.addProperty(new Property("2", "b", "b", "b"));
        Iterator<Property> iterator = instance.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(iterator.next().getId(), "1");
        assertTrue(iterator.hasNext());
        assertEquals(iterator.next().getId(), "2");
        assertFalse(iterator.hasNext());
    }

    /**
     * Verifies that we can set the list of properties.
     */
    @Test
    public void shouldSetProperties() {
        instance.addProperty(new Property());
        instance.setProperties(Arrays.asList(new Property(), new Property()));
        assertEquals(2, instance.getProperties().size());
    }

   

    /**
     * Verifies that a workflow marshaller can visit a property group.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void shouldAcceptMarshaller() throws Exception {
        Property property1 = new Property("1", "a", "a", "a");
        Property property2 = new Property("2", "b", "b", "b");
        property2.setPropertyType(new PropertyType("3", "c", "c", "c"));
        property2.setValidator(new Validator("4", "d", "d", "d"));
        instance.addProperty(property1);
        instance.addProperty(property2);
        MockWorkflowMarshaller marshaller = new MockWorkflowMarshaller();
        instance.accept(marshaller);
        assertEquals(0, marshaller.getTemplateVisits());
        assertEquals(0, marshaller.getTemplateLeaves());
        assertEquals(1, marshaller.getPropertyGroupVisits());
        assertEquals(1, marshaller.getPropertyGroupLeaves());
        assertEquals(2, marshaller.getPropertyVisits());
        assertEquals(2, marshaller.getPropertyLeaves());
        assertEquals(0, marshaller.getPropertyTypeVisits());
        assertEquals(0, marshaller.getPropertyTypeLeaves());
        assertEquals(0, marshaller.getContractTypeVisits());
        assertEquals(0, marshaller.getContractTypeLeaves());
        assertEquals(1, marshaller.getValidatorVisits());
        assertEquals(1, marshaller.getValidatorLeaves());
        assertEquals(0, marshaller.getRuleVisits());
        assertEquals(0, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }

    /**
     * Verifies that two property groups with different sets of properties are not considered equal.
     */
    @Test
    public void propertyGroupsWithDifferentPropertiesShouldNotBeEqual() {
        PropertyGroup propertyGroup1 = createInstance();
        PropertyGroup propertyGroup2 = createInstance();
        assertTrue(propertyGroup1.equals(propertyGroup2));
        propertyGroup2.addProperty(new Property("3872", "foo", "bar", "baz"));
        assertFalse(propertyGroup1.equals(propertyGroup2));
    }

    /**
     * Verifies that two property groups with different property group types are not considered equal.
     */
    @Test
    public void propertyGroupsWithDifferentGroupTypesShouldNotBeEqual() {
        PropertyGroup propertyGroup1 = createInstance();
        PropertyGroup propertyGroup2 = createInstance();
        assertTrue(propertyGroup1.equals(propertyGroup2));
        propertyGroup2.setGroupType("Blarg!");
        assertFalse(propertyGroup1.equals(propertyGroup2));
    }

    /**
     * Verifies that the properties are included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeProperties() {
        PropertyGroup propertyGroup1 = createInstance();
        PropertyGroup propertyGroup2 = createInstance();
        assertTrue(propertyGroup1.hashCode() == propertyGroup2.hashCode());
        propertyGroup2.addProperty(new Property("3872", "foo", "bar", "baz"));
        assertFalse(propertyGroup1.hashCode() == propertyGroup2.hashCode());
    }

    /**
     * Verifies that the group type is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeGroupType() {
        PropertyGroup propertyGroup1 = createInstance();
        PropertyGroup propertyGroup2 = createInstance();
        assertTrue(propertyGroup1.hashCode() == propertyGroup2.hashCode());
        propertyGroup2.setGroupType("Blarg!");
        assertFalse(propertyGroup1.hashCode() == propertyGroup2.hashCode());
    }

    /**
     * Verifies that the property group type is validated in the setter.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateGroupTypeLength() {
        createInstance().setGroupType(longString(256));
    }
}
