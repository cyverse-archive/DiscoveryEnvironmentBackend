package org.iplantc.workflow.experiment.property;

import static org.junit.Assert.assertNull;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.workflow.model.Property;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.experiment.property.FlagPropertyFormatter.
 * 
 * @author Dennis Roberts
 */
public class FlagPropertyFormatterTest extends BasePropertyFormatterTester {

    /**
     * Verifies that we can format a property with a default value.
     */
    @Test
    public void testPropertyWithDefaultValue() {
        JSONObject config = createConfig();
        Property property = createProperty("-foo,-bar", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-bar", 1, "");
    }

    /**
     * Verifies that we can format a property with a specified value.
     */
    @Test
    public void testPropertyWithSpecifiedValue() {
        JSONObject config = createConfig("true");
        Property property = createProperty("-foo,-bar", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-foo", 1, "");
    }

    /**
     * Verifies that we can format a true property with one name.
     */
    @Test
    public void testTruePropertyWithOneName() {
        JSONObject config = createConfig("true");
        Property property = createProperty("-foo", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "-foo", 1, "");
    }

    /**
     * Verifies that we get null for a property with one name and a false value.
     */
    @Test
    public void testFalsePropertyWithOneName() {
        JSONObject config = createConfig("false");
        Property property = createProperty("-foo", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
            createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertNull(formattedProperty);
    }

    /**
     * Verifies that long arguments work correctly when the flag is set to {@code true}.
     */
    @Test
    public void testTruePropertyWithLongName() {
        JSONObject config = createConfig("true");
        Property property = createProperty("--foo=1, --foo=2", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "", 1, "--foo=1");
    }

    /**
     * Verifies that long arguments work correctly when the flag is set to {@code false}.
     */
    @Test
    public void testFalsePropertyWithLongName() {
        JSONObject config = createConfig("false");
        Property property = createProperty("--foo=1, --foo=2", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "", 1, "--foo=2");
    }

    /**
     * Verifies that long arguments with spaces after the equal sign work correctly when the flag is set to
     * {@code true}.
     */
    @Test
    public void testTruePropertyWithLongNameAndSpaces() {
        JSONObject config = createConfig("true");
        Property property = createProperty("--foo= 1, --foo= 2", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "", 1, "--foo=1");
    }

    /**
     * Verifies that long arguments with spaces after the equal sign work correctly when the flag is set to
     * {@code false}.
     */
    @Test
    public void testFalsePropertyWithLongNameAndSpaces() {
        JSONObject config = createConfig("false");
        Property property = createProperty("--foo= 1, --foo= 2", "false", 1, "Selection");
        FlagPropertyFormatter formatter = new FlagPropertyFormatter(config, createStep(), property,
                createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "", 1, "--foo=2");
    }
}
