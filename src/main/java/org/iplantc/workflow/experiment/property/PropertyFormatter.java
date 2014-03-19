package org.iplantc.workflow.experiment.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.util.SfJsonUtils;

/**
 * Formats properties for submission to the job execution framework.
 * 
 * @author Dennis Roberts
 */
public abstract class PropertyFormatter {

    /**
     * The experiment configuration.
     */
    protected JSONObject config;

    /**
     * The transformation step.
     */
    protected TransformationStep step;

    /**
     * The property being formatted.
     */
    protected Property property;

    /**
     * A map of property names to property values.
     */
    protected Map<String, List<String>> propertyValues;

    /**
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public PropertyFormatter(JSONObject config, TransformationStep step, Property property,
        Map<String, List<String>> propertyValues)
    {
        this.config = config;
        this.step = step;
        this.property = property;
        this.propertyValues = propertyValues;
    }

    /**
     * Gets the value of the property.
     * 
     * @return the property value.
     */
    protected String getValue() {
        String value = getValueFromConfig();
        if (value == null) {
            value = getValueFromTransformation();
        }
        if (value == null) {
            value = property.getDefaultValue();
        }
        return value;
    }

    /**
     * Gets the value of the property from the configuration.
     * 
     * @return the property value or null if the property value isn't in the configuration.
     */
    private String getValueFromConfig() {
        String value = null;
        if (!config.isNullObject()) {
            String key = step.getName() + "_" + property.getId();
            if (SfJsonUtils.contains(config, key)) {
                value = config.getString(key);
            }
        }
        return value;
    }

    /**
     * Gets the property value from the transformation.
     * 
     * @return the property value or null if the property value isn't in the transformation.
     */
    private String getValueFromTransformation() {
        String value = null;
        Transformation transformation = step.getTransformation();
        if (transformation != null) {
            value = transformation.getPropertyValues().get(property.getId());
        }
        return value;
    }

    /**
     * Registers the value of the current property.
     * 
     * @param value the property value.
     */
    protected void registerPropertyValue(String value) {
        String key = step.getName() + "_" + property.getName();
        List<String> values = propertyValues.get(key);
        if (values == null) {
            values = new ArrayList<String>();
            propertyValues.put(key, values);
        }
        values.add(value);
    }

    /**
     * Formats the property.
     * 
     * @return the formatted property
     */
    public JSON formatProperty() {
        JSONObject json = new JSONObject();
        json.put("order", property.getOrder());
        json.put("name", property.getName());
        json.put("id", property.getId());
        return json;
    }
}
