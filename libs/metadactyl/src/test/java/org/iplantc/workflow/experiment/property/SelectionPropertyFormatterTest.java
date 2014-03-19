package org.iplantc.workflow.experiment.property;

import static org.junit.Assert.assertNull;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.workflow.model.Property;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.experiment.property.SelectionPropertyFormat.
 * 
 * @author Dennis Roberts
 */
public class SelectionPropertyFormatterTest extends BasePropertyFormatterTester {

    /**
     * Verifies that the formatter works with an empty property value.
     */
    @Test
    public void testEmptyPropertyValue() {
        JSONObject config = createConfig("");
        Property property = createProperty("", 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertNull(formattedProperty);
    }

    /**
     * Verifies that we can format a property with a default value.
     */
    @Test
    public void testPropertyWithDefaultValue() {
        JSONObject config = createConfig();
        Property property = createProperty("-foo,-bar,-baz", propertyJson("-bar", "bar"), 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-bar", 2, "bar");
    }

    /**
     * Verifies that we can format a property with a specified value.
     */
    @Test
    public void testPropertyWithSpecifiedValue() {
        JSONObject config = createConfig(propertyJson("-baz", "baz"));
        Property property = createProperty("-foo,-bar,-baz", propertyJson("-bar", "bar"), 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-baz", 2, "baz");
    }

    /**
     * Verifies that we can format a property without a property name or default value.
     */
    @Test
    public void testPropertyWithEmptyNameAndValue() {
        JSONObject config = createConfig(propertyJson("-baz", "baz"));
        Property property = createProperty("", "", 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(),
                property, createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-baz", 2, "baz");
    }

    /**
     * Verifies that an attempt to format an unspecified property produces a null result.
     */
    @Test
    public void testUnspecifiedProperty() {
        JSONObject config = createConfig(propertyJson("", ""));
        Property property = createProperty("-foo,-bar,", propertyJson("-foo", "foo"), 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(),
            property, createPropertyValueMap());
        assertNull(formatter.formatProperty());
    }

    /**
     * Verifies that the code correctly handles a missing option name.
     */
    @Test
    public void testMissingOptionName() {
        JSONObject config = createConfig(propertyJson("", "baz"));
        Property property = createProperty("", "", 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(),
                property, createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "", 2, "baz");
    }

    /**
     * Verifies that the code correctly handles a missing option value.
     */
    @Test
    public void testMissingOptionValue() {
        JSONObject config = createConfig(propertyJson("-baz", ""));
        Property property = createProperty("", "", 2, "Selection");
        SelectionPropertyFormatter formatter = new SelectionPropertyFormatter(config, createStep(),
                property, createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-baz", 2, "");
    }

    /**
     * Creates the JSON string representing the property.
     * 
     * @param name the property name.
     * @param value the property value.
     * @return the formatted property JSON.
     */
    private String propertyJson(String name, String value) {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("value", value);
        return json.toString();
    }
}
