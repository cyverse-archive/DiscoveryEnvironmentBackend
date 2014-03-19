package org.iplantc.workflow.integration.preview;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.marshaler.UiAnalysisMarshaler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert JSON documents in the format consumed by the analysis import service to documents in the format
 * required by the DE.
 *
 * @author Dennis Roberts
 */
public class WorkflowPreviewer {

    /**
     * A registry of imported objects, indexed by name.
     */
    private HeterogeneousRegistry nameRegistry;

    /**
     * A registry of imported objects, indexed by identifier.
     */
    private HeterogeneousRegistry idRegistry;

    /**
     * The factory used to load data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * Used to load deployedComponents from JSON.
     */
    private DeployedComponentLoader deployedComponentLoader;

    /**
     * Used to load templates from JSON.
     */
    private TemplateLoader templateLoader;

    /**
     * Used to load templates from JSON and generate analyses for them.
     */
    private AnalysisGeneratingTemplateLoader analysisGeneratingTemplateLoader;

    /**
     * Used to load analyses from JSON.
     */
    private AnalysisLoader analysisLoader;

    /**
     * Used to load notification sets from JSON.
     */
    private NotificationSetLoader notificationSetLoader;

    /**
     * Used to generate the outgoing JSON.
     */
    private UiAnalysisMarshaler marshaller;

    /**
     * @param daoFactory the factory used to create data access objects.
     */
    public WorkflowPreviewer(DaoFactory daoFactory) {
        initializeRegistries();
        this.daoFactory = daoFactory;
        initializeLoaders();
        initializeMarshaller();
    }

    /**
     * Initializes the object used to generate the outgoing JSON.
     */
    private void initializeMarshaller() {
        marshaller = new UiAnalysisMarshaler(daoFactory);
        marshaller.setRegistry(idRegistry);
    }

    /**
     * Initializes the workflow element loaders.
     */
    private void initializeLoaders() {
        createDeployedComponentLoader();
        createTemplateLoader();
        createAnalysisGeneratingTemplateLoader();
        createAnalyisLoader();
        createNotificationSetLoader();
    }

    /**
     * Creates the object used to load notification sets from JSON.
     */
    private void createNotificationSetLoader() {
        notificationSetLoader = new NotificationSetLoader();
        addRegistries(notificationSetLoader);
    }

    /**
     * Creates the object used to load analyses from JSON.
     */
    private void createAnalyisLoader() {
        analysisLoader = new AnalysisLoader(daoFactory);
        addRegistries(analysisLoader);
    }

    /**
     * Creates the object used to load templates from JSON.
     */
    private void createTemplateLoader() {
        templateLoader = new TemplateLoader(daoFactory);
        addRegistries(templateLoader);
    }

    private void createAnalysisGeneratingTemplateLoader() {
        analysisGeneratingTemplateLoader = new AnalysisGeneratingTemplateLoader(daoFactory);
        addRegistries(analysisGeneratingTemplateLoader);
    }

    /**
     * Creates the object used to load deployed components from JSON.
     */
    private void createDeployedComponentLoader() {
        deployedComponentLoader = new DeployedComponentLoader(daoFactory);
        addRegistries(deployedComponentLoader);
    }

    /**
     * Adds the registries to the given object loader.
     *
     * @param loader the loader to add the registries to.
     */
    private void addRegistries(ObjectLoader loader) {
        loader.setNameRegistry(nameRegistry);
        loader.setIdRegistry(idRegistry);
    }

    /**
     * Initializes the registries for this previewer.
     */
    private void initializeRegistries() {
        nameRegistry = new HeterogeneousRegistryImpl();
        idRegistry = new HeterogeneousRegistryImpl();
    }

    /**
     * Performs the JSON object conversion.
     *
     * @param json the original JSON object.
     * @return the the updated JSON object.
     * @throws JSONException if the original JSON object doesn't meet the requirements.
     */
    public JSONObject preview(JSONObject json) throws JSONException {
        loadObjects(json);
        return marshallAnalyses();
    }

    /**
     * Previews an individual template. A single-step analysis will be generated for the template that is being
     * previewed.
     *
     * @param json the original JSON object.
     * @return the updated JSON object.
     * @throws JSONException if the original JSON object doesn't meet the requirements.
     */
    public JSONObject previewTemplate(JSONObject json) throws JSONException {
        analysisGeneratingTemplateLoader.loadObject(json);
        return marshallAnalyses();
    }

    /**
     * Loads the objects from the incoming JSON object.
     *
     * @param json the incoming JSON object.
     * @throws JSONException if the original JSON doesn't meet the requirements.
     */
    private void loadObjects(JSONObject json) throws JSONException {
        deployedComponentLoader.loadObjectList(json.optJSONArray("components"));
        templateLoader.loadObjectList(json.optJSONArray("templates"));
        analysisLoader.loadObjectList(json.optJSONArray("analyses"));
        notificationSetLoader.loadObjectList(json.optJSONArray("notification_sets"));
    }

    /**
     * Marshalls the analyses that have been loaded.
     *
     * @return the marshalled analyses.
     * @throws WorkflowException if the analysis can't be marshalled.
     */
    private JSONObject marshallAnalyses() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            for (TransformationActivity analysis : nameRegistry.getRegisteredObjects(TransformationActivity.class)) {
                array.put(marshaller.marshal(analysis));
            }
            json.put("analyses", array);
        }
        catch (Exception e) {
            throw new WorkflowException("unable to convert the analyses.", e);
        }
        return json;
    }
}
