package org.iplantc.workflow.integration.json;

import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class TitoIntegrationDatumUnmarshaller implements TitoUnmarshaller<IntegrationDatum> {
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
     * @param json
     * @return
     * @throws JSONException 
     */
    @Override
    public IntegrationDatum fromJson(JSONObject json) throws JSONException {
        IntegrationDatum datum = new IntegrationDatum();
        
        JSONObject implementation = json.getJSONObject("implementation");
        datum.setIntegratorEmail(implementation.getString("implementor_email"));
        datum.setIntegratorName(implementation.getString("implementor"));
        
        return datum;
    }
}
