package org.iplantc.workflow.experiment.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyType;

/**
 * Common methods for all property formatter unit tests. The purpose of the weird name is to prevent this class from
 * being run directly as a unit test.
 * 
 * @author Dennis Roberts
 */
public class BasePropertyFormatterTester {

    /**
     * The step name to use for all unit tests.
     */
    protected static final String STEP_NAME = "stepname";

    /**
     * Creates the property values map.
     * 
     * @return the new property values map.
     */
    protected Map<String, List<String>> createPropertyValueMap() {
        return new HashMap<String, List<String>>();
    }

    /**
     * Creates a transformation step for testing.
     * 
     * @return the transformation step.
     */
    protected TransformationStep createStep() {
        TransformationStep step = new TransformationStep();
        step.setName(STEP_NAME);
        step.setTransformation(new Transformation());
        return step;
    }

    /**
     * Creates a transformation step with the given property value for testing.
     * 
     * @param propertyValue the property value.
     * @return the transformation step.
     */
    protected TransformationStep createStep(String propertyValue) {
        TransformationStep step = createStep();
        step.getTransformation().getPropertyValues().put("id", propertyValue);
        return step;
    }

    /**
     * Creates a property with the given value, order and property type name.
     * 
     * @param value the property value.
     * @param order the property order value.
     * @param propertyTypeName the name of the property type.
     * @return the property.
     */
    protected Property createProperty(String value, int order, String propertyTypeName) {
        Property property = new Property("id", "name", "label", "description");
        property.setDefaultValue(value);
        property.setOrder(order);
        property.setPropertyType(new PropertyType("id", propertyTypeName, "label", "description"));
        return property;
    }

    /**
     * Creates a property with the given value, order, property type name, and omit-if-blank flag.
     * 
     * @param value the property value.
     * @param order the property order value.
     * @param propertyTypeName the name of the property type.
     * @param omitIfBlank the omit-if-blank flag.
     * @return  the property.
     */
    protected Property createProperty(String value, int order, String propertyTypeName, boolean omitIfBlank) {
        Property property = new Property("id", "name", "label", "description");
        property.setDefaultValue(value);
        property.setOrder(order);
        property.setPropertyType(new PropertyType("id", propertyTypeName, "label", "description"));
        property.setOmitIfBlank(omitIfBlank);
        return property;
    }

    /**
     * Creates a property with the given name, value, order and property type name.
     * 
     * @param name the property name.
     * @param value the property value.
     * @param order the property order value.
     * @param propertyTypeName the name of the property type.
     * @return the property.
     */
    protected Property createProperty(String name, String value, int order, String propertyTypeName) {
        Property property = new Property("id", name, "label", "description");
        property.setDefaultValue(value);
        property.setOrder(order);
        property.setPropertyType(new PropertyType("id", propertyTypeName, "label", "description"));
        return property;
    }

    /**
     * Creates an empty configuration object.
     * 
     * @return the configuration object.
     */
    protected JSONObject createConfig() {
        return new JSONObject();
    }

    /**
     * Creates a configuration object, specifying the given value for the property.
     * 
     * @param value the value to specify for the property.
     * @return the configuration object.
     */
    protected JSONObject createConfig(String value) {
        JSONObject config = createConfig();
        config.put("stepname_id", value);
        return config;
    }

    /**
     * Verifies that a JSON object representing a formatted property is valid.
     * 
     * @param json the JSON object representing the formatted property.
     * @param id the expected property identifier.
     * @param name the expected property name.
     * @param order the expected property order.
     * @param value the expected property value.
     */
    protected void assertFormattedPropertyValid(JSONObject json, String id, String name, int order, String value) {
        assertEquals(id, json.getString("id"));
        assertEquals(name, json.getString("name"));
        assertEquals(order, json.getInt("order"));
        assertEquals(value, json.getString("value"));
    }

    /**
     * Verifies that the given JSON is a JSON object.
     * 
     * @param json The JSON object to verify.
     */
    protected void assertJSONObject(JSON json) {
        assertNotNull(json);
        assertFalse(json.isArray());
    }

    /**
     * Verifies that the given JSON is a JSON array.
     * 
     * @param json The JSON array to verify.
     */
    protected void assertJSONArray(JSON json) {
        assertNotNull(json);
        assertTrue(json.isArray());
    }
}
