package org.iplantc.workflow.integration.json;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.Rating;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.model.Template;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.core.TransformationActivityReference;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.service.WorkspaceInitializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert a JSON document representing an analysis to an analysis.
 *
 * @author Dennis Roberts
 */
public class TitoAnalysisUnmarshaller implements TitoUnmarshaller<TransformationActivity> {

    /**
     * Used to generate data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * The registry of named objects.
     */
    private HeterogeneousRegistry registry;

    private TitoIntegrationDatumMashaller integrationDatumUnmarshaller;
    private WorkspaceInitializer workspaceInitializer;

    /**
     * @param daoFactory used to create data access objects.
     * @param registry the registry of named objects.
     */
    public TitoAnalysisUnmarshaller(DaoFactory daoFactory, HeterogeneousRegistry registry) {
        this.daoFactory = daoFactory;
        this.registry = registry;
        this.integrationDatumUnmarshaller = new TitoIntegrationDatumMashaller();
    }

    /**
     * Converts a JSON object to an analysis.
     *
     * @param json the JSON object.
     * @return the analysis.
     * @throws JSONException if the JSON object we receive is invalid.
     */
    @Override
    public TransformationActivity fromJson(JSONObject json) throws JSONException {
        TransformationActivity analysis = new TransformationActivity();

        analysis.setId(getId(json, "analysis_id", "id"));
        analysis.setName(json.getString("analysis_name"));
        analysis.setDescription(json.getString("description"));
        analysis.setType(json.optString("type", ""));
        analysis.setDeleted(json.optBoolean("deleted", false));
        analysis.setWikiurl(json.optString("wiki_url"));
        analysis.setSteps(stepListFromJson(json.getJSONArray("steps")));
        analysis.setMappings(mappingListFromJson(analysis, json.optJSONArray("mappings")));

        analysis.setIntegrationDatum(getIntegrationDatum(json));

        Date editedDate = getDate(json.optString("edited_date"));
        if (editedDate != null) {
            analysis.setEditedDate(editedDate);
        }

        Date integrationDate = getDate(json.optString("published_date"));
        if (integrationDate != null) {
            analysis.setIntegrationDate(integrationDate);
        }

        analysis.setReferences(getReferences(json));
        analysis.setRatings(unmarshalRatings(json));

        return analysis;
    }

	/**
	 * Gets the integration datum to use for this analysis.  If a matching integration datum already exists then that
	 * one will be used.  Otherwise, a new one will be created.
	 *
	 * @param json the JSON object representing the analysis.
	 * @return the matching integration datum.
	 * @throws JSONException if a JSON error occurs.
	 */
	private IntegrationDatum getIntegrationDatum(JSONObject json) throws JSONException {
		IntegrationDatum integrationDatum = integrationDatumUnmarshaller.fromJson(json);
		IntegrationDatum existing = daoFactory.getIntegrationDatumDao()
				.findByNameAndEmail(integrationDatum.getIntegratorName(), integrationDatum.getIntegratorEmail());
		return existing == null ? integrationDatum : existing;
	}

    private Date getDate(String timestamp) {
        try {
            return new Date(Long.parseLong(timestamp));
        } catch (Exception ignore) {
            return null;
        }
    }

    private Set<TransformationActivityReference> getReferences(JSONObject input) throws JSONException {
        Set<TransformationActivityReference> results = new HashSet<TransformationActivityReference>();

        if(input.has("references")) {
            JSONArray references = input.getJSONArray("references");
            for (int i = 0; i < references.length(); i++) {
                TransformationActivityReference ref = new TransformationActivityReference();
                ref.setReferenceText(references.getString(i));

                results.add(ref);
            }
        }

        return results;
    }

    /**
     * Gets an identifier from the given JSON object. If the identifier isn't specified or is set to "auto-gen" then a
     * new one will be generated.
     *
     * @param json the JSON object describing the analysis that is being imported.
     * @param fieldNames the acceptable names of the field containing the identifier.
     * @return the identifier.
     * @throws JSONException if a JSON error occurs.
     */
    private String getId(JSONObject json, String... fieldNames) throws JSONException {
        String id = JsonUtils.nonEmptyOptString(json, null, fieldNames);
        if (StringUtils.isBlank(id) || id.equals("auto-gen")) {
            id = ImportUtils.generateId();
        }
        return id;
    }

    /**
     * Converts a JSON array to a list of mappings.
     *
     * @param analysis the analysis that is currently being unmarshalled.
     * @param array the JSON array.
     * @return the list of mappings.
     * @throws JSONException if the JSON array is invalid.
     */
    private List<InputOutputMap> mappingListFromJson(TransformationActivity analysis, JSONArray array)
        throws JSONException
    {
        List<InputOutputMap> mappings;
        if (array != null) {
            TitoInputOutputMapUnmarshaller unmarshaller = new TitoInputOutputMapUnmarshaller(analysis);
            mappings = unmarshaller.unmarshall(array);
        }
        else {
            mappings = new LinkedList<InputOutputMap>();
        }
        return mappings;
    }

    /**
     * Converts a JSON array to a list of transformation steps.
     *
     * @param array the JSON array.
     * @return the list of transformation steps.
     * @throws JSONException if the JSON array is invalid.
     */
    private List<TransformationStep> stepListFromJson(JSONArray array) throws JSONException {
        List<TransformationStep> steps = new LinkedList<TransformationStep>();
        for (int i = 0; i < array.length(); i++) {
            steps.add(stepFromJson(array.getJSONObject(i)));
        }
        return steps;
    }

    /**
     * Converts a JSON object to a transformation step.
     *
     * @param json the JSON object.
     * @return the transformation step.
     * @throws JSONException if the JSON object is invalid.
     */
    private TransformationStep stepFromJson(JSONObject json) throws JSONException {
        TransformationStep step = new TransformationStep();
        step.setGuid(getId(json, "id").trim());
        step.setName(json.getString("name").trim());
        step.setDescription(json.optString("description", "").trim());
        step.setTransformation(transformationFromTransformationStepJson(json));
        return step;
    }

    /**
     * Generates a transformation from values in a JSON object that describes a transformation step.
     *
     * @param json the JSON object.
     * @return the transformation.
     * @throws JSONException if the JSON object is invalid.
     */
    private Transformation transformationFromTransformationStepJson(JSONObject json) throws JSONException {
        Transformation transformation = new Transformation();
        String templateId = getTemplateId(json);
        validateTemplateId(templateId);
        transformation.setName("");
        transformation.setTemplate_id(templateId);

        Map<String, String> valuesMap = propertyValueMapFromTransformationStepJson(json.getJSONObject("config"));

        for(String key : valuesMap.keySet()) {
            transformation.addPropertyValue(key, valuesMap.get(key));
        }

        return transformation;
    }

    /**
     * Gets the template identifier for the given transformation step.
     *
     * @param json the JSON object representing the transformation step.
     * @return the template identifier.
     * @throws JSONException if the template identifier can't be found.
     */
    private String getTemplateId(JSONObject json) throws JSONException {
        String templateId = json.optString("template_id", null);
        if (templateId == null && registry != null) {
            templateId = getNamedTemplateId(json.optString("template_ref", null));
        }
        if (templateId == null) {
            String msg = "unable to determine the template identifier for the analysis step; please verify that the "
                + "\"template_id\" or \"template_ref\" attribute is specified correctly";
            throw new JSONException(msg);
        }
        return templateId;
    }

    /**
     * Gets the identifier of the template with the given name.
     *
     * @param templateName the name of the template.
     * @return the template ID or null if the template can't be found.
     */
    private String getNamedTemplateId(String templateName) {
        Template template = registry.get(Template.class, templateName);
        return template == null ? null : template.getId();
    }

    /**
     * Validates a template identifier.
     *
     * @param templateId the template identifier.
     */
    private void validateTemplateId(String templateId) {
        if (!templateInRegistry(templateId) && !templateInDatabase(templateId)) {
            throw new WorkflowException("no template with identifier, " + templateId + " found");
        }
    }

    /**
     * Determines whether or not a template with the given template ID is in the registry.
     *
     * @param templateId the template identifier.
     * @return true if the template is in the registry.
     */
    private boolean templateInRegistry(String templateId) {
        boolean retval = false;
        if (registry != null) {
            for (Template template : registry.getRegisteredObjects(Template.class)) {
                if (StringUtils.equals(templateId, template.getId())) {
                    retval = true;
                    break;
                }
            }
        }
        return retval;
    }

    /**
     * Determines whether or not a template with the given template ID is in the database.
     *
     * @param templateId the template identifier.
     * @return true if the template is in the database.
     */
    private boolean templateInDatabase(String templateId) {
        return daoFactory.getTemplateDao().findById(templateId) != null;
    }

    /**
     * Converts a JSON object to a property value map.
     *
     * @param json the JSON object.
     * @return the property value map.
     * @throws JSONException if the JSON object is invalid.
     */
    private Map<String, String> propertyValueMapFromTransformationStepJson(JSONObject json) throws JSONException {
        Map<String, String> propertyValues = new HashMap<String, String>();
        JSONArray names = json.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                propertyValues.put(name, json.getString(name));
            }
        }
        return propertyValues;
    }

    public Set<Rating> unmarshalRatings(JSONObject json) throws JSONException {
        Set<Rating> result = new HashSet<Rating>();

        // If the workspace initializer is null then we don't care about ratings.
        if (workspaceInitializer == null) {
            return result;
        }

        try {
            JSONArray ratingsArray = json.getJSONArray("ratings");

            for (int i = 0; i < ratingsArray.length(); i++) {
                JSONObject jsonRating = ratingsArray.getJSONObject(i);

                Rating rating = new Rating();
                rating.setRaiting(jsonRating.getInt("rating"));

                // Grab the user's workspace for a User record.  Note that this
                // will create the user and workspace if they don't already exist.
                String username = jsonRating.getString("username");
                Workspace workspace = workspaceInitializer.getWorkspace(daoFactory, username);

                rating.setUser(workspace.getUser());
                result.add(rating);
            }
        } catch(JSONException jsonException) {
            // Rating data may not have been included.  Ignore
        }

        return result;
    }

    public WorkspaceInitializer getWorkspaceInitializer() {
        return workspaceInitializer;
    }

    public void setWorkspaceInitializer(WorkspaceInitializer workspaceInitializer) {
        this.workspaceInitializer = workspaceInitializer;
    }

    public void setDaoFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    public DaoFactory getDaoFactory() {
        return daoFactory;
    }
}
