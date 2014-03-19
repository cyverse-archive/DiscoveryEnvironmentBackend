package org.iplantc.workflow.experiment;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.util.SfJsonUtils;

/**
 * Formats the string used to send environment variable settings to the JEX.
 * 
 * @author Dennis Roberts
 */
public class CondorEnvironmentVariableFormatter {

    /**
     * The job submission template.
     */
    private Template template;
    
    /**
     * The name of the analysis step.
     */
    private String stepName;

    /**
     * The transformation that is to be applied to the job submission template.
     */
    private Transformation transformation;
    
    /**
     * The user-specified property values.
     */
    private JSONObject propertyValues;

    /**
     * @param template the job submission template.
     * @param stepName the name of the analysis step.
     * @param transformation the transformation that is to be applied to the job submission template.
     * @param propertyValues the user-specified property values.
     */
    public CondorEnvironmentVariableFormatter(Template template, String stepName, Transformation transformation,
            JSONObject propertyValues) {
        this.template = template;
        this.stepName = stepName;
        this.transformation = transformation;
        this.propertyValues = propertyValues;
    }

    /**
     * Formats the JSON object to use to pass environment variable values to the JEX.
     * 
     * @return a JSON object whose keys and values are environment variable names and values, respectively.
     */
    public JSONObject format() {
        JSONObject result = new JSONObject();
        for (PropertyGroup group : template.getPropertyGroups()) {
            for (Property prop : group.getProperties()) {
                if (prop.getPropertyTypeName().equals("EnvironmentVariable")) {
                    String value = getPropertyValue(prop);
                    if (value != null) {
                        result.put(prop.getName(), getPropertyValue(prop));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Obtains the value of a property.
     * 
     * @param prop the property.
     * @return the property value.
     */
    private String getPropertyValue(Property prop) {
        String value;
        if (transformation.containsProperty(prop.getId())) {
            value = transformation.getValueForProperty(prop.getId());
        }
        else {
            String key = stepName + "_" + prop.getId();
            value = SfJsonUtils.contains(propertyValues, key) ? propertyValues.getString(key) : prop.getDefaultValue();
        }
        return prop.getOmitIfBlank() && StringUtils.isBlank(value) ? null : value;
    }
}
