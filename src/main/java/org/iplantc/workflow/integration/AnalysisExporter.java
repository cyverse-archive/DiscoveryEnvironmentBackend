package org.iplantc.workflow.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.json.TitoAnalysisMarshaller;
import org.iplantc.workflow.integration.json.TitoDeployedComponentMarshaller;
import org.iplantc.workflow.integration.json.TitoMarshaller;
import org.iplantc.workflow.integration.json.TitoNotificationSetMarshaller;
import org.iplantc.workflow.integration.json.TitoTemplateMarshaller;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to export existing analyses in the database to JSON.
 *
 * @author Dennis Roberts
 */
public class AnalysisExporter {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * Used to marshal deployed components.
     */
    private TitoDeployedComponentMarshaller deployedComponentMarshaller;

    /**
     * Used to marshal templates.
     */
    private TitoTemplateMarshaller templateMarshaller;

    /**
     * Used to marshal analyses.
     */
    private TitoAnalysisMarshaller analysisMarshaller;

    /**
     * Used to marshal notification sets.
     */
    private TitoNotificationSetMarshaller notificationSetMarshaller;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public AnalysisExporter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
        initializeMarshalers();
    }

    /**
     * Initializes the marshalers used by this exporter.
     */
    private void initializeMarshalers() {
        deployedComponentMarshaller = new TitoDeployedComponentMarshaller();
        templateMarshaller = new TitoTemplateMarshaller(daoFactory, true);
        analysisMarshaller = new TitoAnalysisMarshaller(daoFactory, true);
        notificationSetMarshaller = new TitoNotificationSetMarshaller(daoFactory, true);
    }

    /**
     * Exports the analysis with the given identifier.
     *
     * @param analysisId the analysis identifier.
     * @return a JSON object representing the analysis and all external elements used by it.
     */
    public JSONObject exportAnalysis(String analysisId) {
        TransformationActivity analysis = loadAnalysis(analysisId);
        try {
            return exportAnalysis(analysis);
        }
        catch (JSONException e) {
            throw new WorkflowException("error producing JSON object", e);
        }
    }

    /**
     * Loads the analysis with the given identifier.
     *
     * @param analysisId the analysis identifier.
     * @return the analysis.
     * @throws WorkflowException if the analysis isn't found.
     */
    private TransformationActivity loadAnalysis(String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis with ID, " + analysisId + ", not found");
        }
        return analysis;
    }

    /**
     * Exports an analysis.
     *
     * @param analysis the analysis to export.
     * @return a JSON object representing the analysis and all external elements used by it.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject exportAnalysis(TransformationActivity analysis) throws JSONException {
        Set<Template> templates = loadTemplatesInAnalysis(analysis);
        Set<DeployedComponent> components = loadComponents(extractComponentIds(templates));
        List<NotificationSet> notificationSets = loadNotificationSets(analysis);

        JSONObject json = new JSONObject();
        JsonUtils.putIfNotNull(json, "components", marshalElements(deployedComponentMarshaller, components));
        JsonUtils.putIfNotNull(json, "templates", marshalElements(templateMarshaller, templates));
        JsonUtils.putIfNotNull(json, "analyses", marshalElements(analysisMarshaller, Arrays.asList(analysis)));
        JsonUtils.putIfNotNull(json, "notification_sets", marshalElements(notificationSetMarshaller, notificationSets));
        return json;
    }

    /**
     * Marshals a collection of workflow elements.
     *
     * @param <T> the type of workflow element being marshaled.
     * @param marshaller the marshaller used to marshal the elements.
     * @param elements the collection of elements.
     * @return a JSON array containing the marshaled elements or null if the collection is empty.
     */
    private <T> JSONArray marshalElements(TitoMarshaller<T> marshaller, Collection<T> elements) {
        JSONArray result = null;
        if (elements.size() > 0) {
            result = new JSONArray();
            for (T element : elements) {
                result.put(marshaller.toJson(element));
            }
        }
        return result;
    }

    /**
     * Loads the notification sets for the given analysis.
     *
     * @param analysis the analysis.
     * @return the list of notification sets.
     */
    private List<NotificationSet> loadNotificationSets(TransformationActivity analysis) {
        return daoFactory.getNotificationSetDao().findNotificationSetsForAnalysis(analysis);
    }

    /**
     * Loads the deployed components with the given identifiers.
     *
     * @param componentIds the set of deployed component identifiers.
     * @return the set of deployed components.
     */
    private Set<DeployedComponent> loadComponents(Set<String> componentIds) {
        Set<DeployedComponent> components = new HashSet<DeployedComponent>();
        for (String componentId : componentIds) {
            components.add(loadComponent(componentId));
        }
        return components;
    }

    /**
     * Loads the component with the given identifier.
     *
     * @param componentId the component identifier.
     * @return the component identifier.
     * @throws WorkflowException if the component can't be found.
     */
    private DeployedComponent loadComponent(String componentId) {
        DeployedComponent component = daoFactory.getDeployedComponentDao().findById(componentId);
        if (component == null) {
            throw new WorkflowException("no deployed component with ID, " + componentId + ", found");
        }
        return component;
    }

    /**
     * Extracts the set of component identifiers used by the given set of templates. Any duplicates will be removed
     * by virtue of the fact that a set is being returned.
     *
     * @param templates the set of templates.
     * @return the set of component identifiers.
     */
    private Set<String> extractComponentIds(Set<Template> templates) {
        Set<String> componentIds = new HashSet<String>();
        for (Template template : templates) {
            componentIds.add(template.getComponent());
        }
        return componentIds;
    }

    /**
     * Loads the templates in the given analysis.
     *
     * @param analysis the analysis.
     * @return the set of templates.
     */
    private Set<Template> loadTemplatesInAnalysis(TransformationActivity analysis) {
        Set<Template> templates = new HashSet<Template>();
        for (String templateId : daoFactory.getTransformationActivityDao().getTemplateIdsInAnalysis(analysis)) {
            templates.add(loadTemplate(templateId));
        }
        return templates;
    }

    /**
     * Loads the template with the given identifier.
     *
     * @param templateId the template identifier.
     * @return the template.
     * @throws WorkflowException if the template can't be loaded.
     */
    private Template loadTemplate(String templateId) {
        Template template = daoFactory.getTemplateDao().findById(templateId);
        if (template == null) {
            throw new WorkflowException("no template with ID, " + templateId + ", found");
        }
        return template;
    }
}
