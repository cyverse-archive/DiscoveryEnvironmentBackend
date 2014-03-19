package org.iplantc.workflow.experiment.property;

import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.experiment.ParamUtils;
import org.iplantc.workflow.model.Property;

/**
 * The property formatter to use for TreeSelection arguments.
 * 
 * @author psarando
 */
public class TreeSelectionPropertyFormatter extends SelectionPropertyFormatter {

    /**
     * @param config the experiment configuration.
     * @param step the transformation step.
     * @param property the property being formatted.
     * @param propertyValues a map of property names to property values.
     */
    public TreeSelectionPropertyFormatter(JSONObject config, TransformationStep step, Property property,
                                          Map<String, List<String>> propertyValues) {
        super(config, step, property, propertyValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSON formatProperty() {
        JSONArray result = null;

        JSONArray paramArray = ParamUtils.jsonArrayFromString(getValue());
        if (paramArray != null) {
            result = new JSONArray();
            for (int i = 0; i < paramArray.size(); i++) {
                JSONObject param = formatProperty(paramArray.getJSONObject(i));
                CollectionUtils.addIgnoreNull(result, param);
            }
        }

        return result;
    }
}
