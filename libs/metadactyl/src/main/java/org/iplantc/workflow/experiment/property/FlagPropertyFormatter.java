package org.iplantc.workflow.experiment.property;

import static org.iplantc.workflow.experiment.ParamUtils.setParamNameAndValue;

import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.model.Property;

/**
 * The property formatter to use for Flag arguments.
 *
 * @author Dennis Roberts
 */
public class FlagPropertyFormatter extends PropertyFormatter {

    /**
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public FlagPropertyFormatter(JSONObject config, TransformationStep step, Property property,
        Map<String, List<String>> propertyValues)
    {
        super(config, step, property, propertyValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSON formatProperty() {
        JSONObject json = null;
        String selectedOption = determineSelectedOption();
        if (selectedOption != null) {
            String[] components = selectedOption.split("\\s+|(?<==)", 2);
            String name = components[0];
            String value = components.length == 2 ? components[1] : null;
            registerPropertyValue(value);
            json = new JSONObject();
            json.put("order", property.getOrder());
            json.put("id", property.getId());
            setParamNameAndValue(json, name, value);
        }
        return json;
    }

    /**
     * Determines the name to use for the formatted property.
     *
     * @return the name to use.
     */
    private String determineSelectedOption() {
        String name;
        String[] possibleNames = property.getName().split("\\s*,\\s*");
        boolean enabled = Boolean.parseBoolean(getValue());
        if (possibleNames.length == 1) {
            name = enabled ? possibleNames[0] : null;
        }
        else {
            name = enabled ? possibleNames[0] : possibleNames[1];
        }
        return name;
    }
}
