package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.UnitTestUtils.longString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.iplantc.workflow.mock.MockWorkflowElement;
import org.iplantc.workflow.util.FieldLengthValidationException;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for all descendants of org.iplantc.workflow.model.WorkflowElement.
 * 
 * @author Dennis Roberts
 * 
 * @param <T> the type of the class being tested.
 */
public abstract class WorkflowElementTest<T extends WorkflowElement> {

    /**
     * The workflow element to use for each of the tests.
     */
    protected T instance;

    /**
     * Creates an instance of the type of workflow element to test.
     * 
     * @return the new instance.
     */
    protected abstract T createInstance();

    /**
     * Creates an instance of the type of workflow element to test with the given identifier name, label and
     * description.
     * 
     * @param id the workflow element identifier.
     * @param name the workflow element name.
     * @param label the label used to identify the workflow element in the UI.
     * @param description a brief description of the workflow element.
     * @return the new instance.
     */
    protected abstract T createInstance(String id, String name, String label, String description);

    /**
     * Gets the expected identifier of the workflow element being tested.
     * 
     * @return the workflow element identifier.
     */
    protected abstract String getElementId();

    /**
     * Gets the expected name of the workflow element being tested.
     * 
     * @return the workflow element name.
     */
    protected abstract String getElementName();

    /**
     * Gets the expected label of the workflow element being tested.
     * 
     * @return the workflow element label.
     */
    protected abstract String getElementLabel();

    /**
     * Gets the expected description of the workflow element being tested.
     * 
     * @return the workflow element description.
     */
    protected abstract String getElementDescription();

    /**
     * Initializes each test.
     */
    @Before
    public void initialize() {
        instance = createInstance();
    }

    /**
     * Verifies that we can get the property type ID.
     */
    @Test
    public void shouldGetId() {
        assertEquals(getElementId(), instance.getId());
    }

    /**
     * Verifies that we can get the property type name.
     */
    @Test
    public void shouldGetName() {
        assertEquals(getElementName(), instance.getName());
    }

    /**
     * Verifies that we can get the property type label.
     */
    @Test
    public void shouldGetLabel() {
        assertEquals(getElementLabel(), instance.getLabel());
    }

    /**
     * Verifies that we can get the property type description.
     */
    @Test
    public void shouldGetDescription() {
        assertEquals(getElementDescription(), instance.getDescription());
    }

    /**
     * Verifies that equals() can identify equal workflow elements.
     */
    @Test
    public void equalsShouldDetectEqualObjects() {
        assertTrue(createInstance("1", "a", "b", "c").equals(createInstance("1", "a", "b", "c")));
        assertTrue(createInstance("1", null, "b", "c").equals(createInstance("1", null, "b", "c")));
        assertTrue(createInstance("1", "a", null, "c").equals(createInstance("1", "a", null, "c")));
        assertTrue(createInstance("1", "a", "b", null).equals(createInstance("1", "a", "b", null)));
    }

    /**
     * Verifies that equals() detects unequal identifiers.
     */
    @Test
    public void equalsShouldDetectUnequalIds() {
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("2", "a", "b", "c")));
    }

    /**
     * Verifies that equals() detects unequal names.
     */
    @Test
    public void equalsShouldDetectUnequalNames() {
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("1", "b", "b", "c")));
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("1", null, "b", "c")));
        assertFalse(createInstance("1", null, "b", "c").equals(createInstance("1", "a", "b", "c")));
    }

    /**
     * Verifies that equals() detects unequal labels.
     */
    @Test
    public void equalsShouldDetectUnequalLabels() {
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("1", "a", "c", "c")));
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("1", "a", null, "c")));
        assertFalse(createInstance("1", "a", null, "c").equals(createInstance("1", "a", "b", "c")));
    }

    /**
     * Verifies that equals() detects unequal descriptions.
     */
    @Test
    public void equalsShouldDetectUnequalDescriptions() {
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("1", "a", "b", "d")));
        assertFalse(createInstance("1", "a", "b", "c").equals(createInstance("1", "a", "b", null)));
        assertFalse(createInstance("1", "a", "b", null).equals(createInstance("1", "a", "b", "c")));
    }

    /**
     * Verifies that different types of workflow elements are not considered to be equal.
     */
    @Test
    public void equalsShouldDetectDifferentObjects() {
        assertFalse(createInstance("1", "a", "b", "c").equals(new MockWorkflowElement("1", "a", "b", "c")));
    }

    /**
     * Verifies that the identifier is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeId() {
        assertTrue(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "b", "c").hashCode());
        assertFalse(createInstance("1", "a", "b", "c").hashCode() == createInstance("2", "a", "b", "c").hashCode());
    }

    /**
     * Verifies that the name is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeName() {
        assertTrue(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "b", "c").hashCode());
        assertFalse(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "b", "b", "c").hashCode());
    }

    /**
     * Verifies that the label is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeLabel() {
        assertTrue(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "b", "c").hashCode());
        assertFalse(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "c", "c").hashCode());
    }

    /**
     * Verifies that the description is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeDescription() {
        assertTrue(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "b", "c").hashCode());
        assertFalse(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "b", "d").hashCode());
    }

    /**
     * Verifies that the workflow element type is included in the hash code calculation.
     */
    @Test
    public void hashCodeShouldIncludeClass() {
        assertTrue(createInstance("1", "a", "b", "c").hashCode() == createInstance("1", "a", "b", "c").hashCode());
        assertFalse(createInstance("1", "a", "b", "c").hashCode() == new MockWorkflowElement("1", "a", "b", "c").hashCode());
    }

    /**
     * Verifies that the identifier length is validated in the constructor.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateIdLengthInConstructor() {
        createInstance(longString(256), "b", "c", "d");
    }

    /**
     * Verifies that the identifier length is validated in the setter.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateIdLengthInSetter() {
        createInstance("a", "b", "c", "d").setId(longString(256));
    }

    /**
     * Verifies that the name length is validated in the constructor.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateNameLength() {
        createInstance("a", longString(256), "c", "d");
    }

    /**
     * Verifies that the name length is validated in the setter.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateNameLengthInSetter() {
        createInstance("a", "b", "c", "d").setName(longString(256));
    }

    /**
     * Verifies that the description length is validated in the constructor.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateDescriptionLengthInConstructor() {
        createInstance("a", "b", "c", longString(256));
    }

    /**
     * Verifies that the description length is validated in the setter.
     */
    @Test(expected = FieldLengthValidationException.class)
    public void shouldValidateDescriptionLengthInSetter() {
        createInstance("a", "b", "c", "d").setDescription(longString(256));
    }
}
