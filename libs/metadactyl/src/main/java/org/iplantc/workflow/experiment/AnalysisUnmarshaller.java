package org.iplantc.workflow.experiment;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.step.TransformationStep;

import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.persistence.dto.transformation.Transformation;

public class AnalysisUnmarshaller {
	public TransformationActivity unmarshallTransformationActivity(JSONObject json) throws Exception {
		TransformationActivity transformationActivity = new TransformationActivity();
        
		if(json.has("id")) {
			transformationActivity.setId(json.getString("id"));
		} else {
			transformationActivity.setId(json.getString("analysis_id"));
		}

		if(json.has("name")) {
			transformationActivity.setName(json.getString("name"));
		} else {
			transformationActivity.setName(json.getString("analysis_name"));
		}
        
		transformationActivity.setDescription(json.getString("description"));
		transformationActivity.setType(json.optString("type",""));

		JSONArray steps = json.getJSONArray("steps");

		for(int i = 0; i < steps.size(); i++) {
			TransformationStep step = unmarshallStep(steps.getJSONObject(i));
			transformationActivity.addStep(step);
		}

		return transformationActivity;
	}

	public TransformationStep unmarshallStep(JSONObject json) throws Exception{
		TransformationStep step = new TransformationStep();

		step.setGuid(json.getString("id"));
		step.setName(json.getString("name"));
		step.setDescription(json.getString("description"));

		Transformation t = unmarshallTransformation(json.getJSONObject("config"));

		step.setTransformation(t);

		t.setTemplate_id(json.getString("template_id"));

		return step;
	}

	public Transformation unmarshallTransformation(JSONObject json) throws Exception{
		Transformation transformation = new Transformation();

		transformation.setName("no_name");
		transformation.setDescription("transformation");

        for(Object key : json.keySet()) {
            String strKey = key.toString();
            transformation.addPropertyValue(strKey, json.getString(strKey));
        }

		return transformation;
	}

	public InputOutputMap unmarshallMapping(JSONObject json) throws Exception{
		InputOutputMap map = new InputOutputMap();

		return map;
	}
}
