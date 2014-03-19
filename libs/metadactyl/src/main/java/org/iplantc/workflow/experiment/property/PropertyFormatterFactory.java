package org.iplantc.workflow.experiment.property;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.model.Property;

/**
 * Used to generate property formatters.
 *
 * @author Dennis Roberts
 */
public class PropertyFormatterFactory {

    /**
     * Generates the property formatter for the given property type name.
     *
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     * @return the property formatter.
     */
    public static PropertyFormatter getFormatter(JSONObject config, TransformationStep step, Property property,
        Map<String, List<String>> propertyValues)
    {
        PropertyFormatter formatter = null;
        String propertyTypeName = property.getPropertyTypeName();
        if (StringUtils.equals(propertyTypeName, "BarcodeSelector")) {
            formatter = new UnsupportedPropertyFormatter(config, step, property, propertyValues);
        }
        else if (StringUtils.equals(propertyTypeName, "ClipperSelector")) {
            formatter = new UnsupportedPropertyFormatter(config, step, property, propertyValues);
        }
        else if (StringUtils.equals(propertyTypeName, "Flag")) {
            formatter = new FlagPropertyFormatter(config, step, property, propertyValues);
        }
        else if (isSelectionProperty(propertyTypeName)) {
            formatter = new SelectionPropertyFormatter(config, step, property, propertyValues);
        }
        else if (StringUtils.equals(propertyTypeName, "TreeSelection")) {
            formatter = new TreeSelectionPropertyFormatter(config, step, property, propertyValues);
        }
        else if (StringUtils.equals(propertyTypeName, "Input")) {
            formatter = new SkipPropertyFormatter(config, step, property, propertyValues);
        }
        else if (StringUtils.equals(propertyTypeName, "Output")) {
            formatter = new OutputPropertyFormatter(config, step, property, propertyValues);
        }
        else {
            formatter = new DefaultPropertyFormatter(config, step, property, propertyValues);
        }
        return formatter;
    }

    /**
     * Determines whether or not the given property type name refers to a selection property.
     *
     * @param propertyTypeName the property type name.
     * @return true if the property type name refers to a selection property.
     */
    private static boolean isSelectionProperty(String propertyTypeName) {
        boolean result = false;
        if (propertyTypeName != null) {
            result = propertyTypeName.endsWith("Selection");
        }
        return result;
    }
}
