package org.iplantc.workflow.experiment.property;

import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.model.Property;

/**
 * The property formatter to use for Output arguments.
 *
 * @author psarando
 */
public class OutputPropertyFormatter extends DefaultPropertyFormatter {

    /**
     * @param config         the experiment configuration.
     * @param step           the transformation step.
     * @param property       the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public OutputPropertyFormatter(JSONObject config, TransformationStep step, Property property,
                                   Map<String, List<String>> propertyValues) {
        super(config, step, property, propertyValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSON formatProperty() {
        if (property.getDataObject().isImplicit()) {
            return null;
        } else {
            return super.formatProperty();
        }
    }
}
