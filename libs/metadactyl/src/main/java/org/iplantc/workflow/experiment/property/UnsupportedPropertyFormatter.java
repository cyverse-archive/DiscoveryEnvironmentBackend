package org.iplantc.workflow.experiment.property;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.step.TransformationStep;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.model.Property;

/**
 * The property formatter to use for unsupported property types.
 * 
 * @author Dennis Roberts
 */
public class UnsupportedPropertyFormatter extends PropertyFormatter {

    /**
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public UnsupportedPropertyFormatter(JSONObject config, TransformationStep step, Property property,
        Map<String, List<String>> propertyValues)
    {
        super(config, step, property, propertyValues);
        String msg = "unsupported property type, " + property.getPropertyTypeName() + ", for execution type";
        throw new WorkflowException(msg);
    }
}
