package org.iplantc.workflow.integration.json;

import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.workflow.WorkflowException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kris Healy &lt;healyk@iplantcollaborative.org&gt;
 */
public class TitoIntegrationDatumMashaller implements TitoUnmarshaller<IntegrationDatum>, TitoMarshaller<IntegrationDatum> {
    public static final String IMPLEMENTOR = "implementor";
    public static final String IMPLEMENTOR_EMAIL = "implementor_email";

    /**
     * Converts a JSON object to a integration datum.  The integration data (under
     * implementation) is expected to be an object contained by the json object passed:
     * 
     * <code>
     * json: {
     *    "implementation": { ... }
     * }
     * </code>
     * 
     * @param json the JSON object representing the integration datum.
     * @return the integration datum.
     * @throws JSONException if one of the required fields is missing.
     */
    @Override
    public IntegrationDatum fromJson(JSONObject json) throws JSONException {
        IntegrationDatum datum = new IntegrationDatum();
        
        JSONObject implementation = json.getJSONObject("implementation");
        datum.setIntegratorEmail(implementation.getString(IMPLEMENTOR_EMAIL));
        datum.setIntegratorName(implementation.getString(IMPLEMENTOR));
        
        return datum;
    }

    @Override
    public JSONObject toJson(IntegrationDatum object) {
        try {
            JSONObject result = new JSONObject();
            
            result.put(IMPLEMENTOR, object.getIntegratorName());
            result.put(IMPLEMENTOR_EMAIL, object.getIntegratorEmail());
            
            return result;
        } catch (JSONException ex) {
            throw new WorkflowException("Unable to serialize IntegrationDatum to json.", ex);
        }
    }
}
