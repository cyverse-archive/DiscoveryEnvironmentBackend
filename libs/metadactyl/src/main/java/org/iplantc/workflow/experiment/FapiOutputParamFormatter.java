package org.iplantc.workflow.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.data.DataObject;

import static org.iplantc.workflow.experiment.ParamUtils.setParamNameAndValue;

/**
 * Formats output parameters for jobs that are submitted to the Foundational API.
 * 
 * @author Dennis Roberts
 */
public class FapiOutputParamFormatter {

    /**
     * The transformation step that is currently being formatted.
     */
    private TransformationStep step;

    /**
     * A map of property names to property values.
     */
    private Map<String, List<String>> propertyValues;

    /**
     * @param step the transformation step that is currently being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public FapiOutputParamFormatter(TransformationStep step, Map<String, List<String>> propertyValues) {
        this.step = step;
        this.propertyValues = propertyValues;
    }

    /**
     * Adds parameters to the parameter list for the given output.
     * 
     * @param params the list of parameters.
     * @param output the current output.
     */
    public void addParamsForOutput(JSONArray params, DataObject output) {
        if (outputRequiresParameter(output)) {
            params.add(formatParamForOutput(output));
        }
    }

    /**
     * Formats the command-line parameter for the given output.
     * 
     * @param output the output that is currently being formatted.
     * @return the output.
     */
    private JSONObject formatParamForOutput(DataObject output) {
        String value = getOutputValue(output);
        registerPropertyValue(output.getId(), value);
        JSONObject param = new JSONObject();
        setParamNameAndValue(param, output.getSwitchString(), value);
        param.put("order", getOutputOrder(output));
        param.put("id", output.getId());
        param.put("multiplicity", output.getMultiplicityName());
        return param;
    }

    /**
     * Registers a property value.
     * 
     * @param outputId the output identifier.
     * @param value the output value.
     */
    private void registerPropertyValue(String outputId, String value) {
        String key = step.getName() + "_" + outputId;
        List<String> values = propertyValues.get(key);
        if (values == null) {
            values = new ArrayList<String>();
            propertyValues.put(key, values);
        }
        values.add(value);
    }

    /**
     * Gets the command-line order specifier for the given output. This method is only called after we know that the
     * output requires a command-line parameter, so the order should never be negative.
     * 
     * @param output the output that is currently being formatted.
     * @return the output order.
     */
    private Object getOutputOrder(DataObject output) {
        return Math.max(0, output.getOrderd());
    }

    /**
     * Gets the value to use for the given output.
     * 
     * @param output the output.
     * @return the value.
     */
    private String getOutputValue(DataObject output) {
        String retval = null;
        if (step.getTransformation().containsProperty(output.getId())) {
            retval = step.getTransformation().getValueForProperty(output.getId());
        }
        else {
            retval = output.getName();
        }
        return retval;
    }

    /**
     * Determines whether or not the current output requires a command-line parameter.
     * 
     * @param output the output that is currently being formatted.
     * @return true if the output requires a command-line parameter.
     */
    private boolean outputRequiresParameter(DataObject output) {
        return output.getOrderd() >= 0 || !StringUtils.isBlank(output.getSwitchString());
    }
}
