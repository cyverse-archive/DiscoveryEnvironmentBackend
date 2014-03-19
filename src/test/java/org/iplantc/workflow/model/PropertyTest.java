package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.UnitTestUtils.longString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.iplantc.workflow.util.FieldLengthValidationException;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.model.Property.
 * 
 * @author Dennis Roberts
 */
public class PropertyTest extends WorkflowElementTest<Property> {

    // The properties to use for each of the tests.
    private static final String ID = "123";
    private static final String NAME = "someProperty";
    private static final String LABEL = "Some Property";
    private static final String DESCRIPTION = "The description of some property.";
    private static final PropertyType PROPERTY_TYPE = new PropertyType();
    private static final boolean IS_VISIBLE = true;
    private static final boolean OMIT_IF_BLANK = true;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Property createInstance() {
        Property property = new Property(ID, NAME, LABEL, DESCRIPTION);
        populateProperty(property);
        return property;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Property createInstance(String id, String name, String label, String description) {
        Property property = new Property(id, name, label, description);
        populateProperty(property);
        return property;
    }

    /**
     * Populates a property with a property type and validator.
     * 
     * @param property the property to populate.
     */
    private void populateProperty(Property property) {
        property.setPropertyType(PROPERTY_TYPE);
        Validator validator = new Validator();
        validator.addRule(new Rule());
        validator.addRule(new Rule());
        property.setValidator(validator);
        property.setIsVisible(IS_VISIBLE);
        property.setOmitIfBlank(OMIT_IF_BLANK);
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
     * Verifies that we can get the property type.
     */
    @Test
    public void shouldGetPropertyType() {
        assertSame(PROPERTY_TYPE, instance.getPropertyType());
    }

    /**
     * Verifies that we can get the visibility flag.
     */
    @Test
    public void shouldGetIsVisible() {
        assertTrue(instance.getIsVisible());
    }

    /**
     * Verifies that a marshaller can visit a property.
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
        assertEquals(1, marshaller.getPropertyVisits());
        assertEquals(1, marshaller.getPropertyLeaves());
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
     * Verifies that two properties with different property types are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentPropertyTypes() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.equals(property2));
        property2.setPropertyType(new PropertyType("8427", "foo", "bar", "baz"));
        assertFalse(property1.equals(property2));
    }

    /**
     * Verifies that two properties with different validators are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentValidators() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.equals(property2));
        property2.setValidator(new Validator("8427", "foo", "bar", "baz"));
        assertFalse(property1.equals(property2));
    }

    /**
     * Verifies that two properties with different visibility flag values are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentVisibilityFlags() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.equals(property2));
        property2.setIsVisible(!IS_VISIBLE);
        assertFalse(property1.equals(property2));
    }

    /**
     * Verifies that two properties with different default values are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentDefaultValues() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.equals(property2));
        property2.setDefaultValue("some arbitrary value");
        assertFalse(property1.equals(property2));
    }

    /**
     * Verifies that two properties with different omit-if-blank flags are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentOmitIfBlankFlags() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.equals(property2));
        assertTrue(property2.equals(property1));
        property2.setOmitIfBlank(false);
        assertFalse(property1.equals(property2));
        assertFalse(property2.equals(property1));
    }

    /**
     * Verifies that the property type is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludePropertyType() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.hashCode() == property2.hashCode());
        property2.setPropertyType(new PropertyType("8427", "foo", "bar", "baz"));
        assertFalse(property1.hashCode() == property2.hashCode());
    }

    /**
     * Verifies that the validator is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeValidator() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.hashCode() == property2.hashCode());
        property2.setValidator(new Validator("8427", "foo", "bar", "baz"));
        assertFalse(property1.hashCode() == property2.hashCode());
    }

    /**
     * Verifies that the visibility flag is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeVisibilityFlag() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.hashCode() == property2.hashCode());
        property2.setIsVisible(!IS_VISIBLE);
        assertFalse(property1.hashCode() == property2.hashCode());
    }

    /**
     * Verifies that the default value is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeDefaultValue() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.hashCode() == property2.hashCode());
        property2.setDefaultValue("some arbitrary value");
        assertFalse(property1.hashCode() == property2.hashCode());
    }

    /**
     * Verifies that the omit-if-blank flag is included in the hash code.
     */
    @Test
    public void hashCodeShouldIncludeOmitIfBlankFlag() {
        Property property1 = createInstance();
        Property property2 = createInstance();
        assertTrue(property1.hashCode() == property2.hashCode());
        property2.setOmitIfBlank(false);
        assertFalse(property2.hashCode() == property1.hashCode());
    }

    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateDefaultValueLength() {
        createInstance().setDefaultValue(longString(256));
    }
}
