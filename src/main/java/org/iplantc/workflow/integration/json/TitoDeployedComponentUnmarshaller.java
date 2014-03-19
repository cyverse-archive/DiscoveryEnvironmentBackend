package org.iplantc.workflow.integration.json;

import java.util.Set;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.persistence.dto.components.ToolType;
import org.iplantc.persistence.dto.data.DeployedComponentDataFile;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.workflow.UnknownToolTypeException;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert JSON documents describing deployed components to Deployed Components.
 *
 * @author Dennis Roberts
 */
public class TitoDeployedComponentUnmarshaller extends AbstractTitoDataFileUnmarshaller<DeployedComponentDataFile>
        implements TitoUnmarshaller<DeployedComponent> {

	private DaoFactory daoFactory;

    private TitoIntegrationDatumMashaller integrationDatumUnmarshaller;

    public TitoDeployedComponentUnmarshaller(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
		this.integrationDatumUnmarshaller = new TitoIntegrationDatumMashaller();
    }

    /**
     * Creates a new deployed component from the given JSON object.
     *
     * @param json the JSON object describing the deployed component.
     * @return the deployed component.
     * @throws JSONException if the JSON object is missing a required attribute.
     */
    @Override
    public DeployedComponent fromJson(JSONObject json) throws JSONException {
        DeployedComponent deployedComponent = new DeployedComponent();
        deployedComponent.setId(ImportUtils.getId(json, "id"));
        deployedComponent.setName(json.getString("name"));
        deployedComponent.setToolType(getToolType(json.getString("type")));
        deployedComponent.setLocation(json.getString("location"));
        deployedComponent.setDescription(json.optString("description", null));
        deployedComponent.setVersion(json.optString("version", null));
        deployedComponent.setAttribution(json.optString("attribution", null));

        deployedComponent.setIntegrationDatum(getIntegrationDatum(json));
        deployedComponent.setDeployedComponentDataFiles(unmarshallDataFiles(json));

        return deployedComponent;
    }

    /**
     * Gets the selected tool type for the deployed component.
     *
     * @param name the tool type name.
     * @return the tool type.
     * @throws UnknownToolTypeException if a matching tool type isn't found.
     */
    private ToolType getToolType(String name) {
        ToolType result = daoFactory.getToolTypeDao().findByName(name);
        if (result == null) {
            throw new UnknownToolTypeException("name", name);
        }
        return result;
    }

    /**
	 * Gets the integration datum for the deployed component.  If a matching integration datum already exists then that
	 * one will be used.  Otherwise, a new integration datum will be created.
	 *
	 * @param json the JSON object representing the deployed component.
	 * @return the integration datum.
	 * @throws JSONException if a JSON error occurs.
	 */
	private IntegrationDatum getIntegrationDatum(JSONObject json) throws JSONException {
		IntegrationDatum integrationDatum = integrationDatumUnmarshaller.fromJson(json);
		IntegrationDatum existing = null;
		if (daoFactory != null) {
			existing = daoFactory.getIntegrationDatumDao()
					.findByNameAndEmail(integrationDatum.getIntegratorName(), integrationDatum.getIntegratorEmail());
		}
		return existing == null ? integrationDatum : existing;
	}

    @Override
    protected void unmarshallDataFileList(JSONArray jsonFiles, boolean input, Set<DeployedComponentDataFile> files)
            throws JSONException {
        for (int i = 0; i < jsonFiles.length(); i++) {
            DeployedComponentDataFile dataFile = new DeployedComponentDataFile();

            dataFile.setInputFile(input);
            dataFile.setFilename(jsonFiles.getString(i));
            files.add(dataFile);
        }
    }
}
