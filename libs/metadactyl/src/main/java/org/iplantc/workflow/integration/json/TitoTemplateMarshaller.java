package org.iplantc.workflow.integration.json;

import static org.iplantc.workflow.integration.json.TitoMultiplicityNames.titoMultiplicityName;
import static org.iplantc.workflow.integration.util.JsonUtils.putIfNotNull;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.Rule;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.model.Validator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert an existing template to a JSON document. Whenever there are multiple ways to represent a template,
 * this class attempts to match the JSON that would be produced by TITO.
 *
 * @author Dennis Roberts
 */
public class TitoTemplateMarshaller implements TitoMarshaller<Template> {

    /**
     * Used to look up items in the database.
     */
    private final DaoFactory daoFactory;

    /**
     * True if backward references should be used in the generated JSON.
     */
    private final boolean useReferences;

    /**
     * The identifier retention strategy to use.
     */
    private final IdRetentionStrategy idRetentionStrategy;

    /**
     * Initializes a new instance with the default ID retention strategy (CopyIdRetentionStrategy).
     *
     * @param daoFactory used to obtain data access objects.
     * @param useReferences true if backward references should be used in the resulting JSON.
     */
    public TitoTemplateMarshaller(DaoFactory daoFactory, boolean useReferences) {
        this(daoFactory, useReferences, new CopyIdRetentionStrategy());
    }

    /**
     * @param daoFactory used to obtain data access objects.
     * @param useReferences true if backward references should be used in the resulting JSON.
     * @param idRetentionStrategy the ID retention strategy to use.
     */
    public TitoTemplateMarshaller(DaoFactory daoFactory, boolean useReferences,
            IdRetentionStrategy idRetentionStrategy) {
        this.daoFactory = daoFactory;
        this.useReferences = useReferences;
        this.idRetentionStrategy = idRetentionStrategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson(Template object) {
        return toJson(object, null);
    }

    /**
     * Converts a Template object to a JSON document.
     *
     * @param object the Java object
     * @param app An App for the template, containing additional information such as references.
     * @return a JSON object.
     */
    public JSONObject toJson(Template object, TransformationActivity app) {
        try {
            return marshalTemplate(object, app);
        }
        catch (JSONException e) {
            throw new WorkflowException("error producing JSON object", e);
        }
    }

    /**
     * Converts a template to a JSON object.
     *
     * @param template the template to convert.
     * @param app An App for the template, containing additional information such as references.
     * @return the JSON object.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalTemplate(Template template, TransformationActivity app)
            throws JSONException {
        JSONObject json = new JSONObject();

        json.put("id", idRetentionStrategy.getId(template.getId()));
        json.put("tito", idRetentionStrategy.getId(template.getId()));
        json.put("name", template.getName());
        json.put("label", template.getLabel());
        json.put("description", template.getDescription());
        if (useReferences) {
            json.put("component_ref", getComponentName(template.getComponent()));
        }
        else {
            json.put("component", getComponentName(template.getComponent()));
            json.put("component_id", template.getComponent());
        }
        json.put("type", template.getTemplateType());
        json.put("groups", marshalPropertyGroupContainer(template));
        json.put("edited_date", marshalDate(app == null ? null : app.getEditedDate()));
        json.put("published_date", marshalDate(app == null ? null : app.getIntegrationDate()));
        if (app != null && app.getReferences() != null) {
            TitoAnalysisMarshaller appMarshaller = new TitoAnalysisMarshaller(daoFactory, false);
            json.put("references", appMarshaller.marshalReferences(app.getReferences()));
        }

        return json;
    }

    /**
     * Marshals a date, which may be null.
     *
     * @param date the date to marshal.
     * @return the number of milliseconds since the epoch as a string or the empty string if the date is null.
     */
    private String marshalDate(Date date) {
        return date == null ? "" : String.valueOf(date.getTime());
    }

    /**
     * Gets the name of the deployed component with the given component identifier.
     *
     * @param componentId the component identifier.
     * @return the deployed component name if it's available.
     * @throws WorkflowException if the deployed component identifier can't be found.
     */
    private String getComponentName(String componentId) {
        DeployedComponent component = daoFactory.getDeployedComponentDao().findById(componentId);
        return component == null ? "" : component.getName();
    }

    /**
     * Marshals the property group container element that is required by TITO.
     *
     * @param template the template being marshaled.
     * @return the JSON object.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalPropertyGroupContainer(Template template) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", "--root-PropertyGroupContainer--");
        json.put("name", "");
        json.put("label", "");
        json.put("description", "");
        json.put("isVisible", true);
        json.put("groups", marshalPropertyGroups(template));
        return json;
    }

    /**
     * Marshals the list of property groups.
     *
     * @param template the template being marshaled.
     * @return a JSON array representing the list of property groups.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalPropertyGroups(Template template) throws JSONException {
        JSONArray array = new JSONArray();
        List<DataObject> referencedDataObjects = template.findReferencedDataObjects();
        JsonUtils.putIfNotNull(array, marshalInputPropertyGroup(template, referencedDataObjects));
        for (PropertyGroup group : template.getPropertyGroups()) {
            array.put(marshalPropertyGroup(group));
        }
        JsonUtils.putIfNotNull(array, marshalOutputPropertyGroup(template, referencedDataObjects));
        return array;
    }

    /**
     * Marshals an input property group for old-style inputs.
     *
     * @param template the template being marshaled.
     * @param referencedDataObjects the list of data objects that are referenced by properties in the template.
     * @return the property group or null if there are no old-style inputs.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalInputPropertyGroup(Template template, List<DataObject> referencedDataObjects)
            throws JSONException {
        JSONObject result = null;
        List<DataObject> unreferencedInputs = ListUtils.subtract(template.getInputs(), referencedDataObjects);
        if (unreferencedInputs.size() > 0) {
            result = new JSONObject();
            result.put("id", ImportUtils.generateId());
            result.put("name", "Select data:");
            result.put("label", "Select input data");
            result.put("description", "");
            result.put("type", "step");
            result.put("isVisible", true);
            result.put("properties", marshalInputProperties(unreferencedInputs));
        }
        return result;
    }

    /**
     * Marshals an output property group for old-style outputs.
     *
     * @param template the template being marshaled.
     * @param referencedDataObjects the list of data objects that are referenced by properties in the template.
     * @return the property group or null if there are no old-style outputs.
     * @throws JSONException if a JSON error occurs.
     */
    private Object marshalOutputPropertyGroup(Template template, List<DataObject> referencedDataObjects)
            throws JSONException {
        JSONObject result = null;
        List<DataObject> unreferencedOutputs = ListUtils.subtract(template.getOutputs(), referencedDataObjects);
        if (unreferencedOutputs.size() > 0) {
            result = new JSONObject();
            result.put("id", ImportUtils.generateId());
            result.put("name", "Output files");
            result.put("label", "Output files");
            result.put("description", "");
            result.put("type", "step");
            result.put("isVisible", false);
            result.put("properties", marshalOutputProperties(unreferencedOutputs));
        }
        return result;
    }

    /**
     * Marshals the properties associated with the given inputs.
     *
     * @param inputs the inputs to marshal properties for.
     * @return a JSON array containing the list of input properties.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalInputProperties(List<DataObject> inputs) throws JSONException {
        JSONArray array = new JSONArray();
        for (DataObject input : inputs) {
            array.put(marshalInputProperty(input));
        }
        return array;
    }

    /**
     * Marshals the properties associated with the given outputs.
     *
     * @param outputs the outputs to marshal properties for.
     * @return a JSON array containing the list of output properties.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalOutputProperties(List<DataObject> outputs) throws JSONException {
        JSONArray array = new JSONArray();
        for (DataObject output : outputs) {
            array.put(marshalOutputProperty(output));
        }
        return array;
    }

    /**
     * Marshals a property for the given input.
     *
     * @param input the input.
     * @return the marshaled property.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalInputProperty(DataObject input) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", ImportUtils.generateId());
        json.put("name", input.getSwitchString());
        json.put("label", !StringUtils.isEmpty(input.getLabel()) ? input.getLabel() : input.getName());
        json.put("description", input.getDescription());
        json.put("type", "Input");
        json.put("isVisible", true);
        json.put("value", "");
        json.put("order", input.getOrderd());
        json.put("omit_if_blank", true);
        json.put("data_object", marshalDataObject(input, "Input"));
        return json;
    }

    /**
     * Marshals a property for the given output.
     *
     * @param output the output.
     * @return the marshaled property.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalOutputProperty(DataObject output) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", ImportUtils.generateId());
        json.put("name", output.getSwitchString());
        json.put("label", !StringUtils.isEmpty(output.getLabel()) ? output.getLabel() : output.getName());
        json.put("description", output.getDescription());
        json.put("type", "Output");
        json.put("isVisible", false);
        json.put("value", "");
        json.put("order", output.getOrderd());
        json.put("omit_if_blank", true);
        json.put("data_object", marshalDataObject(output, "Output"));
        return json;
    }

    /**
     * Marshals a single property group.
     *
     * @param group the property group to marshal.
     * @return the marshalled property group.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalPropertyGroup(PropertyGroup group) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", idRetentionStrategy.getId(group.getId()));
        json.put("name", group.getName());
        json.put("label", group.getLabel());
        json.put("description", StringUtils.defaultString(group.getDescription()));
        json.put("type", group.getGroupType());
        json.put("isVisible", group.isVisible());
        json.put("properties", marshalProperties(group.getProperties()));
        return json;
    }

    /**
     * Marshals a list of properties.
     *
     * @param properties the property list.
     * @return the marshalled list of properties.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalProperties(List<Property> properties) throws JSONException {
        JSONArray array = new JSONArray();
        for (Property property : properties) {
            array.put(marshalProperty(property));
        }
        return array;
    }

    /**
     * Marshals a single property.
     *
     * @param property the property to marshal.
     * @return the marshalled property.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalProperty(Property property) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", idRetentionStrategy.getId(property.getId()));
        json.put("name", property.getName());
        json.put("label", property.getLabel());
        json.put("description", property.getDescription());
        json.put("type", property.getPropertyTypeName());
        json.put("isVisible", property.getIsVisible());
        json.put("value", property.getDefaultValue());
        json.put("order", property.getOrder());
        json.put("omit_if_blank", property.getOmitIfBlank());
        putIfNotNull(json, "validator", marshalValidator(property.getValidator()));
        putIfNotNull(json, "data_object", marshalDataObject(property.getDataObject(), property.getPropertyTypeName()));
        return json;
    }

    /**
     * Marshals a single data object.
     *
     * @param dataObject the data object to marshal.
     * @param propertyTypeName = the name of the property type.
     * @return the marshaled data object or null if the data object is null.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalDataObject(DataObject dataObject, String propertyTypeName) throws JSONException {
        JSONObject json = null;
        if (dataObject != null) {
            json = new JSONObject();
            json.put("id", idRetentionStrategy.getId(dataObject.getId()));
            json.put(propertyTypeName.equalsIgnoreCase("input") ? "name" : "output_filename", dataObject.getName());
            json.put("multiplicity", titoMultiplicityName(dataObject));
            json.put("order", dataObject.getOrderd());
            json.put("cmdSwitch", dataObject.getSwitchString());
            json.put("file_info_type", dataObject.getInfoTypeName());
            json.put("file_info_type_id", dataObject.getInfoTypeId());
            json.put("format", dataObject.getDataFormatName());
            json.put("format_id", dataObject.getDataFormatId());
            json.put("data_source", dataObject.getDataSourceName());
            json.put("description", dataObject.getDescription());
            json.put("required", dataObject.isRequired());
            json.put("retain", dataObject.getRetain());
            json.put("is_implicit", dataObject.isImplicit());
        }
        return json;
    }

    /**
     * Marshals a single validator.
     *
     * @param validator the validator to marshal.
     * @return the marshaled validator.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalValidator(Validator validator) throws JSONException {
        JSONObject json = null;
        if (validator != null) {
            json = new JSONObject();
            json.put("id", idRetentionStrategy.getId(validator.getId()));
            json.put("name", validator.getName());
            json.put("required", validator.isRequired());
            putIfNotNull(json, "rules", marshalRules(validator.getRules()));
        }
        return json;
    }

    /**
     * Marshals the list of rules for a validator.
     *
     * @param rules the list of rules.
     * @return the marshaled list of rules.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalRules(List<Rule> rules) throws JSONException {
        JSONArray array = new JSONArray();
        if (rules != null && rules.size() > 0) {
            for (Rule rule : rules) {
                array.put(marshalRule(rule));
            }
        }
        return array;
    }

    /**
     * Marshals a single rule.
     *
     * @param rule the rule to marshal.
     * @return the marshaled rule.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalRule(Rule rule) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(rule.getRuleType().getName(), marshalRuleArgs(rule));
        return json;
    }

    /**
     * Marshals the argument list for a rule.
     *
     * @param rule the rule being marshaled.
     * @return the marshaled argument list.
     */
    private JSONArray marshalRuleArgs(Rule rule) {
        JSONArray array = new JSONArray();

        List<String> args = rule.getArguments();
        if (args != null) {
            for (String arg : args) {
                try {
                    array.put(new Integer(Integer.parseInt(arg.trim())));
                }
                catch (NumberFormatException notInt) {
                    try {
                        array.put(new Double(Double.parseDouble(arg.trim())));
                    }
                    catch (NumberFormatException notDouble) {
                        try {
                            array.put(new JSONObject(arg));
                        } catch (JSONException notJson) {
                            // just store the original string
                            array.put(arg);
                        }
                    }
                }
            }
        }

        return array;
    }
}
