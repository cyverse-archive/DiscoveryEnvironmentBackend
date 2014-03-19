package org.iplantc.workflow.integration;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.json.TitoTemplateUnmarshaller;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.integration.validation.TemplateValidator;
import org.iplantc.workflow.integration.validation.TemplateValidatorFactory;
import org.iplantc.workflow.model.Template;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to import templates from JSON objects. Templates are the the most complex objects that are currently supported
 * by the analysis import service, and are defined by several nested objects. The top-level object is the template
 * itself. Each template contains a list of inputs, a list of property groups and a list of outputs. The list of inputs
 * defines the inputs that are accepted by the tool. Similarly, the list of outputs defines the outputs that are
 * produced by the tool. The property groups define groups of configuration properties that are accepted by the tool. In
 * the case of command-line tools, properties generally correspond to command-line options. Each property group contains
 * a list of properties that may optionally contain a validator, which in turn contains a list of validation rules. The
 * format of the input is:
 *
 * <pre>
 * <code>
 * {   "id": &lt;template_identifier&gt;,
 *     "name": &lt;template_name&gt;,
 *     "component": &lt;component_id&gt;,
 *     "type": &lt;component_type&gt;,
 *     "input": [
 *         {   "id": &lt;input_id&gt;,
 *             "name": &lt;input_name&gt;,
 *             "multiplicity": &lt;input_multiplicity&gt;,
 *             "order": &lt;input_order&gt;,
 *             "switch": &lt;input_command_line_switch&gt;,
 *             "description": &lt;input_description&gt;
 *         },
 *         ...
 *     ],
 *     "groups": [
 *         {   "name": &lt;group_name&gt;,
 *             "label": &lt;group_label&gt;,
 *             "id": &lt;group_id&gt;,
 *             "type": &lt;group_type&gt;,
 *             "properties": [
 *                 {   "name": &lt;property_name&gt;,
 *                     "id": &lt;property_id&gt;,
 *                     "type": &lt;property_type&gt;,
 *                     "label": &lt;property_label&gt;,
 *                     "description": &lt;property_description&gt;,
 *                     "order": &lt;property_order&gt;,
 *                     "value": &lt;property_value&gt;,
 *                     "validator": {
 *                         "id": &lt;validator_id&gt;,
 *                         "name": &lt;validator_name&gt;,
 *                         "required": &lt;true_or_false&gt;,
 *                         "rules": [
 *                             {   &lt;rule_type&gt;: [
 *                                     &lt;rule_argument_1&gt;,
 *                                     &lt;rule_argument_2&gt;,
 *                                     ...,
 *                                     &lt;rule_argument_n&gt;
 *                                 ]
 *                             },
 *                             ...
 *                         ]
 *                     }
 *                 },
 *                 ...
 *             ]
 *         },
 *     ],
 *     "output": [
 *         {    "id": &lt;output_id&gt;,
 *              "name": &lt;output_name&gt;,
 *              "switch": &lt;output_switch&gt;,
 *              "type": &lt;output_type&gt;,
 *              "multiplicity": &lt;output_multiplicity&gt;,
 *              "order": &lt;output_order&gt;,
 *              "description": &lt;output_description&gt;
 *         },
 *         ...
 *     ]
 * }
 * </code>
 * </pre>
 *
 * @author Dennis Roberts
 */
public class TemplateImporter implements ObjectImporter, ObjectVetter<Template> {

    /**
     * The factory used to generate data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * The registry of named workflow elements.
     */
    private HeterogeneousRegistry registry = new NullHeterogeneousRegistry();

    /**
     * Indicates what should be done if an existing template matches the template being imported.
     */
    private UpdateMode updateMode = UpdateMode.DEFAULT;

    /**
     * True if we should allow vetted templates to be updated.
     */
    private boolean updateVetted;

    /**
     * Used when vetting the template.
     */
    private ObjectVetter<TransformationActivity> analysisVetter;

    /**
     * Used to validate templates that are being imported.
     */
    private TemplateValidator templateValidator;

    /**
     * @return the DAO factory.
     */
    protected DaoFactory getDaoFactory() {
        return daoFactory;
    }

    /**
     * @param registry the new registry of named workflow elements.
     */
    public void setRegistry(HeterogeneousRegistry registry) {
        this.registry = registry == null ? new NullHeterogeneousRegistry() : registry;
    }

    /**
     * @return the registry of named workflow elements.
     */
    public HeterogeneousRegistry getRegistry() {
        return registry;
    }

    /**
     * Enables the replacement of existing templates.
     */
    @Override
    public void enableReplacement() {
        setUpdateMode(UpdateMode.REPLACE);
    }

    /**
     * Disables the replacement of existing templates.
     */
    @Override
    public void disableReplacement() {
        setUpdateMode(UpdateMode.THROW);
    }

    /**
     * Tells the importer to ignore attempts to replace existing templates.
     */
    @Override
    public void ignoreReplacement() {
        setUpdateMode(UpdateMode.IGNORE);
    }

    /**
     * Explicitly sets the update mode.
     *
     * @param updateMode the new update mode.
     */
    @Override
    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    /**
     * @param DaoFactory the factory used to generate data access objects.
     */
    public TemplateImporter(DaoFactory daoFactory) {
        this(daoFactory, false, TemplateValidatorFactory.createDefaultTemplateValidator());
    }

    /**
     * @param daoFactory the factory used to generate data access objects.
     * @param updateVetted true if we should allow vetted analyses to be updated.
     */
    public TemplateImporter(DaoFactory daoFactory, boolean updateVetted, TemplateValidator templateValidator) {
        this.daoFactory = daoFactory;
        this.analysisVetter = new AnalysisImporter(daoFactory, null, null);
        this.updateVetted = updateVetted;
        this.templateValidator = templateValidator;
    }

    /**
     * Used to check if a Template has been vetted.
     *
     * @param username
     *  The fully qualified username.
     * @param template
     *  Template to check to see if it's vetted.
     * @return
     *  True if the Template is vetted.
     */
    @Override
    public boolean isObjectVetted(String username, Template template) {
        List<TransformationActivity> analyses =
                daoFactory.getTransformationActivityDao().getAnalysesReferencingTemplateId(template.getId());

        boolean hasVettedAnalysis = false;
        for (TransformationActivity analysis : analyses) {
            if (analysisVetter.isObjectVetted(getUsername(username, analysis), analysis)) {
                hasVettedAnalysis = true;
            }
        }

        return hasVettedAnalysis;
    }

    /**
     * Gets the username of the integrator who is importing the template.  If the username is specified in the
     * JSON then that username is used.  Otherwise, the e-mail address of the analysis integrator is used.
     *
     * @param username the username that was specified in the JSON.
     * @param analysis the analysis.
     * @return the username.
     */
    private String getUsername(String username, TransformationActivity analysis) {
        return StringUtils.isEmpty(username) ? analysis.getIntegrationDatum().getIntegratorEmail() : username;
    }

    /**
     * Imports a template using values from the given JSON object.
     *
     * @param json the JSON object that defines the template to import.
     * @return the template ID.
     * @throws JSONException if the JSON object is invalid.
     */
    @Override
    public String importObject(JSONObject json) throws JSONException {
        Template template = unmarshallTemplate(json);
        templateValidator.validate(template, registry);
        validateTemplate(template);
        Template existingTemplate = null;
        if ("auto-gen".equals(template.getId())) {
            template.setId(ImportUtils.generateId());
        }
        else {
            existingTemplate = findExistingTemplate(template);
        }
        if (existingTemplate == null) {
            saveNewTemplate(template, json);
        }
        else if (updateMode == UpdateMode.REPLACE) {
            if (updateVetted || !isObjectVetted(json.optString("full_username"), existingTemplate)) {
                replaceExistingTemplate(template, existingTemplate, json);
            }
            else {
                throw new VettedWorkflowObjectException("Cannot replace Template because existing template is vetted.");
            }
        }
        else if (updateMode == UpdateMode.THROW) {
            throw new WorkflowException("a duplicate template was found and replacement is not enabled");
        }
        registry.add(Template.class, template.getName(), template);
        return template.getId();
    }

    /**
     * Finds an existing template by ID or name.
     *
     * @param template the template to find the duplicate of.
     * @return the existing template or null if a match isn't found.
     */
    protected Template findExistingTemplate(Template template) {
        Template existingTemplate = null;
        if (!StringUtils.isEmpty(template.getId())) {
            existingTemplate = daoFactory.getTemplateDao().findById(template.getId());
        }
        else if (existingTemplate == null) {
            existingTemplate = daoFactory.getTemplateDao().findUniqueInstanceByName(template.getName());
        }
        return existingTemplate;
    }

    /**
     * Validates a template.
     *
     * @param template the template to validate.
     */
    private void validateTemplate(Template template) {
        validateComponentId(template);
    }

    /**
     * Validates a component identifier in a template.
     *
     * @param template the template to validate.
     */
    private void validateComponentId(Template template) {
        String componentId = template.getComponent();
        if (!StringUtils.isEmpty(componentId) && !componentInRegistry(componentId)
                && !componentInDatabase(componentId)) {
            throw new WorkflowException("component ID " + componentId + " not found");
        }
    }

    /**
     * Determines whether or not the component with the given identifier is in the database.
     *
     * @param componentId the component identifier to search for.
     * @return true if the component is found in the database.
     */
    private boolean componentInDatabase(String componentId) {
        return daoFactory.getDeployedComponentDao().findById(componentId) != null;
    }

    /**
     * Determines whether or not the component with the given identifier is in the registry.
     *
     * @param componentId the component identifier to search for.
     * @return true if the component is found in the registry.
     */
    private boolean componentInRegistry(String componentId) {
        boolean retval = false;
        for (DeployedComponent component : registry.getRegisteredObjects(DeployedComponent.class)) {
            if (StringUtils.equals(componentId, component.getId())) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    /**
     * Saves a new template.
     *
     * @param template the template to save.
     * @param json the JSON object representing the template.
     */
    protected void saveNewTemplate(Template template, JSONObject json) {
        daoFactory.getTemplateDao().save(template);
    }

    /**
     * Replaces an existing template.
     *
     * @param template the new version of the template.
     * @param existingTemplate the existing template.
     * @param json the JSON object representing the template.
     */
    protected void replaceExistingTemplate(Template template, Template existingTemplate, JSONObject json) {
        template.setId(existingTemplate.getId());
        daoFactory.getTemplateDao().delete(existingTemplate);
        saveNewTemplate(template, json);
    }

    /**
     * Unmarshalls a template.
     *
     * @param json the JSON object representing the template.
     * @return the template.
     * @throws JSONException if the JSON object doesn't meet the expectations of the unmarshaller.
     */
    private Template unmarshallTemplate(JSONObject json) throws JSONException {
        TitoTemplateUnmarshaller unmarshaller = new TitoTemplateUnmarshaller(registry, daoFactory);
        Template template = unmarshaller.fromJson(json);
        return template;
    }

    /**
     * Imports a list of templates from the given JSON array.
     *
     * @param array the JSON array that defines the list of templates.
     * @return the list of template IDs.
     * @throws JSONException if the JSON array is invalid.
     */
    @Override
    public List<String> importObjectList(JSONArray array) throws JSONException {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++) {
            result.add(importObject(array.getJSONObject(i)));
        }
        return result;
    }

    /**
     * Used for testing.
     *
     * @param analysisVetter
     */
    public void setAnalysisVetter(ObjectVetter<TransformationActivity> analysisVetter) {
        this.analysisVetter = analysisVetter;
    }
}
