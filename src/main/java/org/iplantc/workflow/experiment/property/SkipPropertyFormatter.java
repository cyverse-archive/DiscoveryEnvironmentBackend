package org.iplantc.workflow.experiment.property;

import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.model.Property;

/**
 * A property formatter that never formats anything.
 * 
 * @author Dennis Roberts
 */
class SkipPropertyFormatter extends PropertyFormatter {

    /**
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public SkipPropertyFormatter(JSONObject config, TransformationStep step, Property property,
            Map<String, List<String>> propertyValues) {
        super(config, step, property, propertyValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSON formatProperty() {
        return null;
    }
}
