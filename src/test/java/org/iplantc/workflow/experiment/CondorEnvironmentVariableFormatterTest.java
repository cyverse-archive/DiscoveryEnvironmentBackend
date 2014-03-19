package org.iplantc.workflow.experiment;

import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.UnitTestUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

/**
 * @author Dennis Roberts
 */
public class CondorEnvironmentVariableFormatterTest {

    /**
     * Verifies that environment variable properties are formatted from default values correctly.
     */
    @Test
    public void testDefaultValues() {
        List<Property> properties = createEnvProperties("FOO", "BAR");
        Template template = createTemplate(properties);
        Transformation transformation = new Transformation();
        CondorEnvironmentVariableFormatter formatter = getFormatter(template, "step", transformation, new JSONObject());
        JSONObject env = formatter.format();
        assertEquals("FOO_VALUE", env.getString("FOO"));
        assertEquals("BAR_VALUE", env.getString("BAR"));
    }

    /**
     * Verifies that environment variable properties are formatted from user-specified values correctly.
     */
    @Test
    public void testUserSpecifiedValues() {
        List<Property> properties = createEnvProperties("FOO", "BAR");
        Template template = createTemplate(properties);
        Transformation transformation = new Transformation();
        JSONObject propValues = createPropValues("step", "FOO", "OOF", "BAR", "RAB");
        CondorEnvironmentVariableFormatter formatter = getFormatter(template, "step", transformation, propValues);
        JSONObject env = formatter.format();
        assertEquals("OOF", env.getString("FOO"));
        assertEquals("RAB", env.getString("BAR"));
    }

    /**
     * Verifies that environment variable properties that should be omitted are and that those that should not be are
     * not.
     */
    @Test
    public void testOmittedEnvironmentVariableProperties() {
        List<Property> properties = createEnvProperties("A", "B", "C");
        properties.get(0).setOmitIfBlank(true);
        properties.get(2).setOmitIfBlank(true);
        Template template = createTemplate(properties);
        Transformation transformation = new Transformation();
        JSONObject propValues = createPropValues("step", "A", "", "B", "", "C", "SEA");
        CondorEnvironmentVariableFormatter formatter = getFormatter(template, "step", transformation, propValues);
        JSONObject env = formatter.format();
        assertFalse(env.containsKey("A"));
        assertEquals("", env.getString("B"));
        assertEquals("SEA", env.getString("C"));
    }

    /**
     * Verifies that values can be obtained from the transformation correctly.
     */
    @Test
    public void testValuesFromTransformation() {
        List<Property> properties = createEnvProperties("FOO", "BAR");
        Template template = createTemplate(properties);
        Transformation transformation = new Transformation();
        transformation.addPropertyValue("FOO", "OOF");
        transformation.addPropertyValue("BAR", "RAB");
        CondorEnvironmentVariableFormatter formatter = getFormatter(template, "step", transformation, new JSONObject());
        JSONObject env = formatter.format();
        assertEquals("OOF", env.getString("FOO"));
        assertEquals("RAB", env.getString("BAR"));
    }

    /**
     * Verifies that environment variable properties that should be omitted are and that those that should not be are
     * not when the values are obtained from the transformation.
     */
    @Test
    public void testOmittedValuesFromTransformation() {
        List<Property> properties = createEnvProperties("A", "B", "C");
        properties.get(0).setOmitIfBlank(true);
        properties.get(2).setOmitIfBlank(true);
        Template template = createTemplate(properties);
        Transformation transformation = new Transformation();
        transformation.addPropertyValue("A", "");
        transformation.addPropertyValue("B", "");
        transformation.addPropertyValue("C", "SEE");
        CondorEnvironmentVariableFormatter formatter = getFormatter(template, "step", transformation, new JSONObject());
        JSONObject env = formatter.format();
        assertFalse(env.containsKey("A"));
        assertEquals("", env.getString("B"));
        assertEquals("SEE", env.getString("C"));
    }

    /**
     * Verifies that non-environment-variable properties are omitted.
     */
    @Test
    public void testNonEnvironmentVariableProperties() {
        List<Property> properties = createEnvProperties("FOO");
        properties.add(createTypedProperty("BAR", "String"));
        Template template = createTemplate(properties);
        Transformation transformation = new Transformation();
        CondorEnvironmentVariableFormatter formatter = getFormatter(template, "step", transformation, new JSONObject());
        JSONObject env = formatter.format();
        assertEquals("FOO_VALUE", env.getString("FOO"));
        assertFalse(env.containsKey("BAR"));
    }

    /**
     * Creates the property values map.
     * 
     * @param stepName the name of the transformation step.
     * @param kvs an array of alternating keys and values.
     * @return the property values map.
     */
    private JSONObject createPropValues(String stepName, String... kvs) {
        JSONObject result = new JSONObject();
        for (int i = 0; i < kvs.length - 1; i += 2) {
            result.put(stepName + "_" + kvs[i], kvs[i + 1]);
        }
        return result;
    }

    /**
     * Creates multiple environment variable properties.
     * 
     * @param names the list of property names.
     * @return the list of properties.
     */
    private List<Property> createEnvProperties(String... names) {
        return ListUtils.map(new Lambda<String, Property>() {
            @Override
            public Property call(String arg) {
                return createEnvProperty(arg);
            }
        }, Arrays.asList(names));
    }

    /**
     * Creates an environment variable property.  The default value is just the name with "_VALUE" appended.
     * 
     * @param name the name of the property, which also corresponds to the name of the environment variable.
     * @return the property.
     */
    private Property createEnvProperty(String name) {
        return createTypedProperty(name, "EnvironmentVariable");
    }

    /**
     * Creates a property of the given type with the given name.
     * 
     * @param name the name of the property.
     * @return the property.
     */
    private Property createTypedProperty(String name, String type) {
        Property property = new Property();
        property.setId(name);
        property.setName(name);
        property.setDefaultValue(name + "_VALUE");
        property.setPropertyType(UnitTestUtils.createPropertyType(type));
        return property;
    }

    /**
     * Creates a template that can be used for testing.
     * 
     * @param properties the list of properties to include in the template.
     * @return the template.
     */
    private Template createTemplate(List<Property> properties) {
        Template template = new Template();
        PropertyGroup group = new PropertyGroup();
        group.setProperties(properties);
        template.addPropertyGroup(group);
        return template;
    }

    /**
     * Creates a new condor environment variable formatter.  This is really just a shortcut for calling the
     * constructor.
     * 
     * @param template the template to format the environment variables for.
     * @param stepName the name of the transformation step.
     * @param transformation the transformation to apply to the template.
     * @param propertyValues a JSON object containing the user-specified property values.
     * @return the formatter.
     */
    private CondorEnvironmentVariableFormatter getFormatter(Template template, String stepName,
            Transformation transformation, JSONObject propertyValues) {
        return new CondorEnvironmentVariableFormatter(template, stepName, transformation, propertyValues);
    }
}
