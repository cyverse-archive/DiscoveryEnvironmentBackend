package org.iplantc.workflow.experiment.property;

import static org.iplantc.workflow.experiment.ParamUtils.setParamNameAndValue;

import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.model.Property;

/**
 * The property formatter to use for Selection arguments.
 * 
 * @author Dennis Roberts
 */
public class SelectionPropertyFormatter extends PropertyFormatter {

    /**
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public SelectionPropertyFormatter(JSONObject config, TransformationStep step, Property property,
        Map<String, List<String>> propertyValues)
    {
        super(config, step, property, propertyValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSON formatProperty() {
        JSONObject result;
        String value = getValue();
        if (StringUtils.isBlank(value)) {
            result = null;
        }
        else {
            result = formatProperty((JSONObject) JSONSerializer.toJSON(value));
        }
        return result;
    }

    /**
     * Formats a new-style property, in which the name and value are specified by a JSON object.
     * 
     * @param propertyJson the JSON object describing the property.
     * @return the formatted property.
     */
    protected JSONObject formatProperty(JSONObject propertyJson) {
        return formatProperty(propertyJson.getString("name"), propertyJson.optString("value"));
    }

    /**
     * Formats a property with the given name and value.
     * 
     * @param name the name (that is, the command-line option) used to identify the property.
     * @param value the property value.
     * @return the formatted property.
     */
    private JSONObject formatProperty(String name, String value) {
        JSONObject json = null;
        if (!StringUtils.isBlank(name) || !StringUtils.isBlank(value)) {
            json = new JSONObject();
            json.put("order", property.getOrder());
            setParamNameAndValue(json, name, value);
            json.put("id", property.getId());
            registerPropertyValue("");
        }
        return json;
    }
}
