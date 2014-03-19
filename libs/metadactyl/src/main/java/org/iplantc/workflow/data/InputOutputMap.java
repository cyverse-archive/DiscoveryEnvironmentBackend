package org.iplantc.workflow.data;

import java.util.HashMap;
import java.util.Map;
import org.iplantc.persistence.dto.step.TransformationStep;

/**
 * This class describes the relationship between the output of one transformation step
 * into the input of another step in a workflow setting.
 * 
 * 
 * @author Juan Antonio Raygoza Garay
 * 
 */
public class InputOutputMap {

	private long hid;
	
	Map<String, String> input_output_relation = new HashMap<String, String>();
	
	TransformationStep source;
	TransformationStep target;
	
	
	public Map<String, String> getInput_output_relation() {
		return input_output_relation;
	}
	public void setInput_output_relation(Map<String, String> input_output_relation) {
		this.input_output_relation = input_output_relation;
	}
	public TransformationStep getSource() {
		return source;
	}
	public void setSource(TransformationStep source) {
		this.source = source;
	}
	public TransformationStep getTarget() {
		return target;
	}
	public void setTarget(TransformationStep target) {
		this.target = target;
	}
	public long getHid() {
		return hid;
	}
	public void setHid(long hid) {
		this.hid = hid;
	}

    public void addAssociation(String sourcePropertyName, String targetPropertyName) {
        input_output_relation.put(sourcePropertyName, targetPropertyName);
    }

    public boolean containsPropertyAsSource(String property) {
        return input_output_relation.containsKey(property);
    }

    public boolean containsPropertyAsTarget(String property){
        return input_output_relation.containsValue(property);
	}
}
