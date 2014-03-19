package org.iplantc.workflow.experiment.property;

import static org.junit.Assert.assertNull;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.experiment.property.OutputPropertyFormatter.
 *
 * @author psarando
 */
public class OutputPropertyFormatterTest extends BasePropertyFormatterTester {

    /**
     * Verifies that we can format an Output property with a default value.
     */
    @Test
    public void testPropertyWithDefaultValue() {
        JSONObject config = createConfig();
        Property property = createProperty("some value", 0, "Output");

        property.setDataObject(new DataObject());

        OutputPropertyFormatter formatter = new OutputPropertyFormatter(config, createStep(), property,
                                                                        createPropertyValueMap());
        JSON formattedProperty = formatter.formatProperty();
        assertJSONObject(formattedProperty);
        assertFormattedPropertyValid((JSONObject)formattedProperty, "id", "name", 0, "some value");
    }

    /**
     * Verifies that we can format an implicit Output property without an order.
     */
    @Test
    public void testPropertyImplicitOutput() {
        JSONObject config = createConfig();
        Property property = createProperty("some value", -1, "Output");

        property.setDataObject(new DataObject());
        property.getDataObject().setImplicit(true);

        OutputPropertyFormatter formatter = new OutputPropertyFormatter(config, createStep(), property,
                                                                        createPropertyValueMap());
        assertNull(formatter.formatProperty());
    }

    /**
     * Verifies that we can format an implicit Output property with an order.
     */
    @Test
    public void testPropertyImplicitOutputOrdered() {
        JSONObject config = createConfig();
        Property property = createProperty("some value", 2, "Output");

        property.setDataObject(new DataObject());
        property.getDataObject().setImplicit(true);

        OutputPropertyFormatter formatter = new OutputPropertyFormatter(config, createStep(), property,
                                                                        createPropertyValueMap());
        assertNull(formatter.formatProperty());
    }
}
