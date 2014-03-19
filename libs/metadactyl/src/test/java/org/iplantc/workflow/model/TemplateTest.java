package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.UnitTestUtils.longString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.iplantc.workflow.mock.MockWorkflowMarshaller;
import org.iplantc.workflow.util.FieldLengthValidationException;
import org.junit.Test;

public class TemplateTest extends WorkflowElementTest<Template> {

    // The property values to use for each of the tests.
    private static final String ID = "4321";
    private static final String NAME = "someTemplate";
    private static final String LABEL = "Some Template Name";
    private static final String DESCRIPTION = "Description of some template.";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Template createInstance() {
        return new Template(ID, NAME, LABEL, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Template createInstance(String id, String name, String label, String description) {
        return new Template(id, name, label, description);
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
     * Verifies that we can add property groups to the template.
     */
    @Test
    public void shouldAddPropertyGroups() {
        instance.addPropertyGroup(new PropertyGroup());
        instance.addPropertyGroup(new PropertyGroup());
        assertEquals(2, instance.getPropertyGroups().size());
    }

    /**
     * Verifies that we can set the list of property groups.
     */
    @Test
    public void shouldSetPropertyGroups() {
        instance.addPropertyGroup(new PropertyGroup());
        instance.setPropertyGroups(Arrays.asList(new PropertyGroup(), new PropertyGroup()));
        assertEquals(2, instance.getPropertyGroups().size());
    }

    /**
     * Verifies that a workflow marshaller can visit a template.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void shouldAcceptMarshaller() throws Exception {
        PropertyGroup propertyGroup1 = new PropertyGroup();
        propertyGroup1.addProperty(new Property());
        propertyGroup1.addProperty(new Property());
        instance.addPropertyGroup(propertyGroup1);
        Template child1 = new Template();
        child1.addPropertyGroup(new PropertyGroup());
        child1.addPropertyGroup(new PropertyGroup());
        MockWorkflowMarshaller marshaller = new MockWorkflowMarshaller();
        instance.accept(marshaller);
        assertEquals(1, marshaller.getTemplateVisits());
        assertEquals(1, marshaller.getTemplateLeaves());
        assertEquals(1, marshaller.getPropertyGroupVisits());
        assertEquals(1, marshaller.getPropertyGroupLeaves());
        assertEquals(2, marshaller.getPropertyVisits());
        assertEquals(2, marshaller.getPropertyLeaves());
        assertEquals(0, marshaller.getPropertyTypeVisits());
        assertEquals(0, marshaller.getPropertyTypeLeaves());
        assertEquals(0, marshaller.getValidatorVisits());
        assertEquals(0, marshaller.getValidatorLeaves());
        assertEquals(0, marshaller.getRuleVisits());
        assertEquals(0, marshaller.getRuleLeaves());
        assertEquals(0, marshaller.getRuleTypeVisits());
        assertEquals(0, marshaller.getRuleTypeLeaves());
    }

    /**
     * Verifies that two templates with different property groups are not considered to be equal.
     */
    @Test
    public void templatesWithDifferentPropertyGroupsShouldBeDifferent() {
        Template template1 = createInstance();
        Template template2 = createInstance();
        assertTrue(template1.equals(template2));
        template2.addPropertyGroup(new PropertyGroup("3428", "foo", "bar", "baz"));
        assertFalse(template1.equals(template2));
    }

    /**
     * Verifies that two templates with different template types are not considered to be equal.
     */
    @Test
    public void templatesWithDifferentTypesShouldBeDifferent() {
        Template template1 = createInstance();
        Template template2 = createInstance();
        assertTrue(template1.equals(template2));
        template2.setTemplateType("some template type");
        assertFalse(template1.equals(template2));
    }

    /**
     * Verifies that the property groups are included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludePropertyGroups() {
        Template template1 = createInstance();
        Template template2 = createInstance();
        assertTrue(template1.hashCode() == template2.hashCode());
        template2.addPropertyGroup(new PropertyGroup("3428", "foo", "bar", "baz"));
        assertFalse(template1.hashCode() == template2.hashCode());
    }

    /**
     * Verifies that the template type is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeTemplateType() {
        Template template1 = createInstance();
        Template template2 = createInstance();
        assertTrue(template1.hashCode() == template2.hashCode());
        template2.setTemplateType("some template type");
        assertFalse(template1.hashCode() == template2.hashCode());
    }

    /**
     * Verifies that setTemplateType validates the field length.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateTemplateTypeLength() {
        createInstance().setTemplateType(longString(256));
    }

    /**
     * Verifies that setType validates the field length.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateTypeLength() {
        createInstance().setType(longString(256));
    }

    /**
     * Verifies that setComponent validates the field length.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateComponentLength() {
        createInstance().setComponent(longString(256));
    }
}
