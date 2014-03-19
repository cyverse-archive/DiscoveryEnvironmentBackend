package org.iplantc.workflow.experiment.property;

import static org.junit.Assert.assertNull;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.workflow.model.Property;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.experiment.property.DefaultPropertyFormatter.
 * 
 * @author Dennis Roberts
 */
public class DefaultPropertyFormatterTest extends BasePropertyFormatterTester {

    /**
     * Verifies that we can format a property with a default value.
     */
    @Test
    public void testPropertyWithDefaultValue() {
        JSONObject config = createConfig();
        Property property = createProperty("some value", 0, "Text");
        DefaultPropertyFormatter formatter = new DefaultPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "name", 0, "some value");
    }

    /**
     * Verifies that we can format a property with a specified value.
     */
    @Test
    public void testPropertyWithSpecifiedValue() {
        JSONObject config = createConfig("some other value");
        Property property = createProperty("some value", 0, "Text");
        DefaultPropertyFormatter formatter = new DefaultPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "name", 0, "some other value");
    }

    /**
     * Verifies that we can format a property for which the value is specified in the transformation.
     */
    @Test
    public void testPropertyWtihValueInTransformation() {
        JSONObject config = createConfig();
        Property property = createProperty("some value", 0, "Text");
        DefaultPropertyFormatter formatter = new DefaultPropertyFormatter(config, createStep("value"), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "name", 0, "value");
    }

    /**
     * Verifies that a property with a blank value is not omitted if its omit-if-blank setting is disabled.
     */
    @Test
    public void testBlankPropertyWithOmitIfBlankFlagDisabled() {
        JSONObject config = createConfig();
        Property property = createProperty("", 0, "Text", false);
        DefaultPropertyFormatter formatter = new DefaultPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "name", 0, "");
    }

    /**
     * Verifies that a property with a blank value is omitted if its omit-if-blank setting is enabled.
     */
    @Test
    public void testBlankPropertyWithOmitIfBlankFlagEnabled() {
        JSONObject config = createConfig();
        Property property = createProperty("", 0, "Text", true);
        DefaultPropertyFormatter formatter = new DefaultPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        assertNull(formatter.formatProperty());
    }
}
