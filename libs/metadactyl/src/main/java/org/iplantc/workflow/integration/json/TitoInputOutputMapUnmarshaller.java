package org.iplantc.workflow.integration.json;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.iplantc.persistence.dto.step.TransformationStep;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.data.InputOutputMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to unmarshall input/output activities for an analysis. Each mapping to be unmarshalled is expected to be in
 * the following format:
 * 
 * <pre>
 * <code>
 * {   "source_step": &lt;source_step_name&gt;,
 *     "target_step": &lt;target_step_name&gt;,
 *     "map": {
 *         &lt;output_id_1&gt;: &lt;input_id_1&gt;,
 *         &lt;output_id_2&gt;: &lt;input_id_2&gt;,
 *         ...,
 *         &lt;output_id_n&gt;: &lt;input_id_n&gt;
 *     }
 * }
 * </code>
 * </pre>
 * 
 * @author Dennis Roberts
 */
public class TitoInputOutputMapUnmarshaller {

    /**
     * The analysis that we're loading mappings for.
     */
    private TransformationActivity analysis;

    /**
     * Initializes a new input output map unmarshaller.
     * 
     * @param analysis the analysis that we're loading mappings for.
     */
    public TitoInputOutputMapUnmarshaller(TransformationActivity analysis) {
        this.analysis = analysis;
    }

    /**
     * Converts a JSON array to a list of input/output mappings.
     * 
     * @param array the JSON array.
     * @return the list of input/output mappings.
     * @throws JSONException if the JSON array is invalid.
     * @throws WorkflowException if one of the step names isn't assoicated with the analysis.
     */
    public List<InputOutputMap> unmarshall(JSONArray array) throws JSONException, WorkflowException {
        List<InputOutputMap> mappings = new LinkedList<InputOutputMap>();
        for (int i = 0; i < array.length(); i++) {
            mappings.add(unmarshall(array.getJSONObject(i)));
        }
        return mappings;
    }

    /**
     * Converts a JSON object to an input/output mapping.
     * 
     * @param json the the JSON object.
     * @return the input/output mapping.
     * @throws JSONException if the JSON object is invalid.
     * @throws WorkflowException if one of the step names isn't associated with the analysis.
     */
    public InputOutputMap unmarshall(JSONObject json) throws JSONException, WorkflowException {
        InputOutputMap mapping = new InputOutputMap();
        mapping.setSource(findStep(json.getString("source_step")));
        mapping.setTarget(findStep(json.getString("target_step")));
        mapping.setInput_output_relation(inputOutputRelationFromJson(json.getJSONObject("map")));
        return mapping;
    }

    /**
     * Converts a JSON object to an input/output relation.
     * 
     * @param json the JSON object.
     * @return the input/output relation.
     * @throws JSONException if the JSON object is invalid.
     */
    private Map<String, String> inputOutputRelationFromJson(JSONObject json) throws JSONException {
        Map<String, String> relation = new HashMap<String, String>();
        JSONArray names = json.names();
        for (int i = 0; i < names.length(); i++) {
            String name = names.getString(i);
            relation.put(name, json.getString(name));
        }
        return relation;
    }

    /**
     * Finds a transformation step by name.
     * 
     * @param name the step name.
     * @return the transformation step.
     */
    private TransformationStep findStep(String name) {
        TransformationStep step = analysis.getStepByName(name);
        if (step == null) {
            throw new WorkflowException("transformation step, " + name + " not found for mapping");
        }
        return step;
    }
}
