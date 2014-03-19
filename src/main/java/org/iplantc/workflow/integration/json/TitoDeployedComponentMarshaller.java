package org.iplantc.workflow.integration.json;

import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.json.JSONException;
import org.json.JSONObject;

public class TitoDeployedComponentMarshaller implements TitoMarshaller<DeployedComponent> {

    private TitoImplementationDataFileMarshaller implementationDataFileMarshaller;

    private TitoIntegrationDatumMashaller integrationDatumMashaller;

    public TitoDeployedComponentMarshaller() {
        super();

        this.implementationDataFileMarshaller = new TitoImplementationDataFileMarshaller();
        this.integrationDatumMashaller = new TitoIntegrationDatumMashaller();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson(DeployedComponent object) {
        try {
            return marshalDeployedComponent(object);
        }
        catch (JSONException e) {
            throw new WorkflowException("error producing JSON object", e);
        }
    }

    /**
     * Marshals a deployed component.
     * 
     * @param deployedComponent the deployed component to marshal.
     * @return the marshaled deployed component.
     */
    private JSONObject marshalDeployedComponent(DeployedComponent deployedComponent) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", deployedComponent.getId());
        json.put("name", deployedComponent.getName());
        json.put("location", deployedComponent.getLocation());
        json.put("type", deployedComponent.getType());
        json.put("description", deployedComponent.getDescription());
        json.put("version", deployedComponent.getVersion());
        json.put("attribution", deployedComponent.getAttribution());

        JSONObject implementation = integrationDatumMashaller.toJson(deployedComponent.getIntegrationDatum());

        implementationDataFileMarshaller.marshalDataFiles(implementation, deployedComponent.
                getDeployedComponentDataFiles());

        json.put("implementation", implementation);

        return json;
    }
}
