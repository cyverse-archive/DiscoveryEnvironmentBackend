package org.iplantc.workflow.marshaler;

import java.util.Stack;

import org.iplantc.workflow.WorkflowException;
import org.json.JSONException;
import org.json.JSONObject;

public class BaseMarshaller {

	/**
	 * The top-level JSON object that we're building. This object will contain all of components that are marshalled.
	 */
	protected JSONObject cumulativeJson = null;

	/**
	 * A stack of JSON objects used to preserve the hierarchical representation of the data in the JSON.
	 */
	protected Stack<JSONObject> jsonStack = new Stack<JSONObject>();



	public String getMarshalledWorkflow() throws WorkflowException {
		if (cumulativeJson == null) {
			throw new WorkflowException("nothing has been marshalled yet");
		}
		return cumulativeJson.toString();
	}

    public JSONObject getCumulativeJson() throws WorkflowException {
        if (cumulativeJson == null) {
            throw new WorkflowException("nothing has been marshalled yet");
        }
        return cumulativeJson;
    }

	/**
	 * Adds the JSON object that is currently being created an array property in the parent object if the parent
	 * exists.
	 *
	 * @param propertyName the name of the property to use.
	 * @param json the JSON object to to append to the parent property.
	 * @throws JSONException if we can't append to the property.
	 */
	public  void appendToParentProperty(String propertyName, JSONObject json) throws JSONException {
		if (!jsonStack.isEmpty()) {
			jsonStack.peek().append(propertyName, json);
		}
	}

	/**
	 * Adds the JSON object that is currently being created to the parent object if the parent exists.
	 *
	 * @param propertyName the name of the property to use.
	 * @param json the JSON object to add to the parent.
	 * @throws JSONException if the property can't be set.
	 */
	protected void setParentProperty(String propertyName, JSONObject json) throws JSONException {
		if (!jsonStack.isEmpty()) {
			jsonStack.peek().put(propertyName, json);
		}
	}


	/**
	 * Creates a JSON object to use in marshalling a workflow. The first JSON object that is created is treated
	 * as the overall JSON object that we're creating.
	 *
	 * @return the JSON object
	 */
	protected JSONObject createJsonObject() {
		JSONObject json = new JSONObject();
		if (cumulativeJson == null) {
			cumulativeJson = json;
		}
		return json;
	}






}
