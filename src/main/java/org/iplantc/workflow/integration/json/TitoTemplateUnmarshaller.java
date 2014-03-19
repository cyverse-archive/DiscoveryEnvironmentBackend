package org.iplantc.workflow.integration.json;

import static org.iplantc.workflow.integration.util.AnalysisImportUtils.getDate;
import static org.iplantc.workflow.integration.util.JsonUtils.optBoolean;
import static org.iplantc.workflow.integration.util.JsonUtils.optString;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.persistence.dto.data.DataSource;
import org.iplantc.workflow.UnknownDataSourceException;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InfoType;
import org.iplantc.workflow.data.Multiplicity;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.PropertyType;
import org.iplantc.workflow.model.Rule;
import org.iplantc.workflow.model.RuleType;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.model.Validator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Converts JSON object representing templates to templates.
 *
 * @author Dennis Roberts
 */
public class TitoTemplateUnmarshaller implements TitoUnmarshaller<Template> {

    public static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String FILE_INFO_TYPE = "File";

    /**
     * The list of accepted multiplicity descriptions. Each element in the list contains a list of synonyms for the
     * multiplicity with the first name in the list.
     */
    public static final String[][] ACCEPTED_MULTIPLICITIES = {
        {"single", "one"},
        {"many", "multiple"},
        {"collection", "folder", "directory"}
    };

    /**
     * The unspecified data format name.
     */
    private static final String UNSPECIFIED_DATA_FORMAT = "Unspecified";

    /**
     * The registry of named workflow elements.
     */
    private HeterogeneousRegistry registry;

    /**
     * Used to generate data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * The property types we've encountered so far, indexed by name.
     */
    private Map<String, PropertyType> propertyTypesByName = new HashMap<String, PropertyType>();

    private String templateId;

    private List<DataObject> inputs;

    private List<DataObject> outputs;

    /**
     * @param registry the registry of named workflow elements.
     * @param daoFactory the factory used to generate data access objects.
     */
    public TitoTemplateUnmarshaller(HeterogeneousRegistry registry, DaoFactory daoFactory) {
        this.registry = registry;
        this.daoFactory = daoFactory;
    }

    /**
     * Converts the given JSON object to a template.
     *
     * @param json the JSON object.
     * @return the template.
     * @throws JSONException if the JSON object is invalid.
     */
    @Override
    public Template fromJson(JSONObject json) throws JSONException {
        templateId = ImportUtils.getId(json, "id");
        inputs = new LinkedList<DataObject>();
        outputs = new LinkedList<DataObject>();

        Template template = new Template();
        template.setId(templateId);
        template.setName(json.optString("name", ""));
        template.setLabel(json.optString("label", template.getName()));
        template.setDescription(json.optString("description", ""));
        template.setComponent(getComponentId(json));
        template.setTemplateType(json.optString("type", ""));
        template.setEditedDate(getDate(json.optString("edited_date")));
        template.setIntegrationDate(getDate(json.optString("published_date")));
        template.setPropertyGroups(extractPropertyGroups(json));

        addOldStyleInputs(inputs, json.optJSONArray("input"));
        addOldStyleOutputs(outputs, json.optJSONArray("output"));

        template.setInputs(inputs);
        template.setOutputs(outputs);
        return template;
    }

    /**
     * Extracts the property group list from a JSON object representing a template. The original JSON format required
     * by the importer has the list of property groups in a "groups" attribute directly underneath the template JSON.
     * The TITO UI needs to retain information about property group containers, however, so property groups are
     * contained within a "groups" attribute of an object that is stored under the "groups" attribute of the template
     * JSON.
     *
     * @param json the JSON object representing the template.
     * @return the list of property groups.
     * @throws JSONException if the JSON object doesn't meet the requirements.
     */
    private List<PropertyGroup> extractPropertyGroups(JSONObject json) throws JSONException {
        JSONObject groupJson = json.optJSONObject("groups");
        JSONArray groupsArray = groupJson == null ? json.getJSONArray("groups") : groupJson.getJSONArray("groups");
        return propertyGroupListFromJson(groupsArray);
    }

    /**
     * Gets the appropriate component ID to use for the template. If the component ID is specified (in the "component"
     * element of the JSON object), use it. Otherwise, use the "component_ref" element as the name of the component to
     * use.
     *
     * @param json the JSON object.
     * @return the component.
     * @throws JSONException if an appropriate component ID can't be found.
     */
    private String getComponentId(JSONObject json) throws JSONException {
        String componentId = StringUtils.defaultIfBlank(optString(json, null, "component_id", "component"), null);
        if (componentId == null && registry != null) {
            componentId = getNamedComponentId(json.optString("component_ref", null));
        }
        return componentId;
    }

    /**
     * Gets the identifier of a named deployed component.
     *
     * @param name the name of the deployed component.
     * @return the component identifier.
     */
    private String getNamedComponentId(String name) throws JSONException {
        DeployedComponent component = registry.get(DeployedComponent.class, name);
        return component == null ? null : component.getId();
    }

    /**
     * Converts the given JSON array to a list of property groups.
     *
     * @param array the JSON array.
     * @return the list of property groups.
     * @throws JSONException if the JSON array is invalid.
     */
    private List<PropertyGroup> propertyGroupListFromJson(JSONArray array) throws JSONException {
        List<PropertyGroup> propertyGroups = new LinkedList<PropertyGroup>();
        for (int i = 0; i < array.length(); i++) {
            propertyGroups.add(propertyGroupFromJson(array.getJSONObject(i)));
        }
        return propertyGroups;
    }

    /**
     * Converts the given JSON object to a property group.
     *
     * @param json the JSON object.
     * @return the property group.
     * @throws JSONException if the JSON object we get is invalid.
     */
    private PropertyGroup propertyGroupFromJson(JSONObject json) throws JSONException {
        PropertyGroup propertyGroup = new PropertyGroup();
        propertyGroup.setId(ImportUtils.getId(json, "id"));
        propertyGroup.setName(json.optString("name", ""));
        propertyGroup.setLabel(json.optString("label", ""));
        propertyGroup.setGroupType(json.optString("type", ""));
        propertyGroup.setVisible(optBoolean(json, true, "visible", "isVisible"));
        propertyGroup.setProperties(propertyListFromJson(json.getJSONArray("properties")));
        return propertyGroup;
    }

    /**
     * Converts the given JSON array to a list of properties.
     *
     * @param array the JSON array.
     * @return the list of properties.
     * @throws JSONException if the JSON array is invalid.
     */
    private List<Property> propertyListFromJson(JSONArray array) throws JSONException {
        List<Property> properties = new LinkedList<Property>();
        for (int i = 0; i < array.length(); i++) {
            properties.add(propertyFromJson(array.getJSONObject(i), i));
        }
        return properties;
    }

    /**
     * Converts the given JSON object to a property.
     *
     * @param json the JSON object.
     * @return the property.
     * @throws JSONException if the JSON object is invalid.
     */
    private Property propertyFromJson(JSONObject json, int listIndex) throws JSONException {
        PropertyType propertyType = getPropertyType(json.getString("type"));
        boolean visible = optBoolean(json, true, "visible", "isVisible");
        if (!visible && !propertyType.isHidable()) {
            throw new IllegalArgumentException("properties of type " + propertyType.getName() + " may not be hidden.");
        }

        Property property = new Property();
        property.setId(ImportUtils.getId(json, "id"));
        property.setPropertyType(propertyType);
        property.setDefaultValue(extractDefaultValue(json));
        property.setName(json.optString("name", ""));
        property.setLabel(json.optString("label", ""));
        property.setDescription(json.optString("description", ""));
        property.setOrder(json.optInt("order", listIndex));
        property.setValidator(validatorFromJson(json.optJSONObject("validator")));
        property.setIsVisible(visible);
        property.setOmitIfBlank(JsonUtils.optBoolean(json, true, "omit_if_blank", "omitIfBlank"));

        if (property.getPropertyTypeName().equalsIgnoreCase("input")) {
            property.setDataObject(inputFromJson(json.optJSONObject("data_object"), listIndex, property.getId()));
            inputs.add(property.getDataObject());
        }
        else if (property.getPropertyTypeName().equalsIgnoreCase("output")) {
            property.setDataObject(outputFromJson(json.optJSONObject("data_object"), listIndex, property.getId()));
            outputs.add(property.getDataObject());
        }

        return property;
    }

    /**
     * Converts the given JSON object to a validator if the JSON object is provided.
     *
     * @param json the JSON object.
     * @return the validator or null if no JSON object is provided.
     * @throws JSONException if the given JSON object is invalid.
     */
    private Validator validatorFromJson(JSONObject json) throws JSONException {
        Validator validator = null;
        if (json != null) {
            validator = new Validator();
            validator.setId(ImportUtils.getId(json, "id"));
            validator.setName(JsonUtils.optString(json, "", "name", "label"));
            validator.setRequired(json.optBoolean("required", false));
            validator.setRules(ruleListFromJson(json.optJSONArray("rules")));
        }
        return validator;
    }

    /**
     * Converts the the given JSON array to a list of rules.
     *
     * @param array the JSON array.
     * @return the rule arguments.
     * @throws JSONException if the JSON array is invalid.
     */
    private List<Rule> ruleListFromJson(JSONArray array) throws JSONException {
        List<Rule> rules = new LinkedList<Rule>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                rules.add(ruleFromJson(array.getJSONObject(i)));
            }
        }
        return rules;
    }

    /**
     * Converts the given JSON object to a rule.
     *
     * @param json the JSON object.
     * @return the rule.
     * @throws JSONException if the JSON object is invalid.
     */
    private Rule ruleFromJson(JSONObject json) throws JSONException {
        Rule rule = new Rule();
        RuleType ruleType = ruleTypeFromRuleJson(json);
        rule.setId(ImportUtils.generateId());
        rule.setRuleType(ruleType);
        rule.setArguments(ruleArgumentListFromJson(json.getJSONArray(ruleType.getName())));
        return rule;
    }

    /**
     * Converts the given JSON array to a list of rule arguments.
     *
     * @param array the JSON array.
     * @return the list of arguments.
     * @throws JSONException if the JSON array is invalid.
     */
    private List<String> ruleArgumentListFromJson(JSONArray array) throws JSONException {
        List<String> arguments = new LinkedList<String>();
        for (int i = 0; i < array.length(); i++) {
            arguments.add(array.get(i).toString());
        }
        return arguments;
    }

    /**
     * Obtains the rule type from the given JSON object. The JSON object is actually the object that defines the rule
     * containing the rule type.
     *
     * @param json the JSON object that describes the rule.
     * @return the rule type.
     * @throws JSONException if the JSON object is invalid.
     */
    private RuleType ruleTypeFromRuleJson(JSONObject json) throws JSONException {
        JSONArray keyNames = json.names();
        if (keyNames.length() != 1) {
            throw new JSONException("unexpected number of elements in rule definition: " + keyNames.length());
        }
        String ruleTypeName = keyNames.getString(0);
        RuleType ruleType = daoFactory.getRuleTypeDao().findUniqueInstanceByName(ruleTypeName);
        if (ruleType == null) {
            throw new JSONException("no rule type with name " + ruleTypeName + " found");
        }
        return ruleType;
    }

    /**
     * Gets the property type with the given name.
     *
     * @param name the property type name.
     * @return the property type.
     */
    private PropertyType getPropertyType(String name) {
        PropertyType propertyType = getPropertyTypeFromLocalCache(name);
        if (propertyType == null) {
            propertyType = getPropertyTypeFromDatabase(name);
        }
        if (propertyType == null) {
            throw new WorkflowException("no property type with name \"" + name + "\" found");
        }
        return propertyType;
    }

    /**
     * Gets the property type with the given name from the database.
     *
     * @param name the name of the property type.
     * @return the property type or null if it can't be found.
     */
    private PropertyType getPropertyTypeFromDatabase(String name) {
        PropertyType propertyType = daoFactory.getPropertyTypeDao().findUniqueInstanceByName(name);
        if (propertyType != null) {
            propertyTypesByName.put(name, propertyType);
        }
        return propertyType;
    }

    /**
     * Gets the property type with the given name form our local cache.
     *
     * @param name the name of the property type.
     * @return the property type or null if it can't be found.
     */
    private PropertyType getPropertyTypeFromLocalCache(String name) {
        return propertyTypesByName.get(name);
    }

    /**
     * Converts the given JSON object to a data object.
     *
     * @param json the JSON representation of a DataObject.
     * @param propertyId the property identifier.
     * @return the data object.
     * @throws JSONException if the JSON object is invalid.
     */
    private DataObject dataObjectFromJson(JSONObject json, int listIndex, String propertyId) throws JSONException {
        if (json == null) {
            return null;
        }

        DataFormat dataFmt = getDataFormat(json.optString("format", null));
        InfoType infoType = getInfoType(JsonUtils.optString(json, "File", "file_info_type", "type"));

        DataObject dataObject = new DataObject();
        dataObject.setId(propertyId);
        dataObject.setMultiplicity(loadMultiplicity(json.getString("multiplicity")));
        dataObject.setOrderd(json.optInt("order", listIndex));
        dataObject.setSwitchString(JsonUtils.optString(json, "", "switch", "option", "param_option", "cmdSwitch"));
        dataObject.setInfoType(infoType);
        dataObject.setDataFormat(dataFmt);
        dataObject.setDescription(json.optString("description", ""));
        dataObject.setRequired(json.optBoolean("required", true));

        return dataObject;
    }

    /**
     * Converts the given JSON object to an output data object.
     *
     * @param json the JSON representation of a DataObject.
     * @param propertyId the property identifier.
     * @return the data object.
     * @throws JSONException if the JSON object is invalid.
     */
    private DataObject outputFromJson(JSONObject json, int listIndex, String propertyId) throws JSONException {
        if (json == null) {
            return null;
        }
        DataObject dataObject = dataObjectFromJson(json, listIndex, propertyId);
        dataObject.setName(JsonUtils.nonEmptyOptString(json, "", "output_filename", "name", "label"));
        dataObject.setRetain(json.optBoolean("retain", true));
        dataObject.setImplicit(json.optBoolean("is_implicit", false));
        dataObject.setDataSource(findDataSource(json.optString("data_source", "file")));
        return dataObject;
    }

    /**
     * Converts the given JSON object to an input data object.
     *
     * @param json the JSON representation of a DataObject.
     * @param propertyId the property identifier.
     * @return the data object.
     * @throws JSONException if the JSON object is invalid.
     */
    private DataObject inputFromJson(JSONObject json, int listIndex, String propertyId) throws JSONException {
        if (json == null) {
            return null;
        }
        DataObject dataObject = dataObjectFromJson(json, listIndex, propertyId);
        dataObject.setName(JsonUtils.nonEmptyOptString(json, "", "name", "label"));
        dataObject.setRetain(json.optBoolean("retain", false));
        dataObject.setDataSource(findDataSource("file"));
        return dataObject;
    }

    /**
     * Finds the data source with the name specified in the JSON.
     *
     * @param name the data source name.
     * @return the data source.
     * @throws UnknownDataSourceException if the data source isn't found.
     */
    private DataSource findDataSource(String name) {
        DataSource dataSource = daoFactory.getDataSourceDao().findByName(name);
        if (dataSource == null) {
            throw new UnknownDataSourceException("name", name);
        }
        return dataSource;
    }

    /**
     * Loads the multiplicity setting, allowing for some variation in the name used to describe the multiplicity
     * setting.
     *
     * @param name the selected multiplicity setting name.
     * @return the multiplicity setting.
     */
    private Multiplicity loadMultiplicity(String name) {
        name = standardizeMultiplicityName(name);
        Multiplicity multiplicity = daoFactory.getMultiplicityDao().findUniqueInstanceByName(name);
        if (multiplicity == null) {
            throw new WorkflowException("no multiplicity setting with name \"" + name + "\" found");
        }
        return multiplicity;
    }

    /**
     * Converts accepted multiplicity names to the actual names in the database.
     *
     * @param name the selected multiplicity name.
     * @return the standardized multiplicity name.
     */
    private String standardizeMultiplicityName(String name) {
        for (String[] synonyms : ACCEPTED_MULTIPLICITIES) {
            if (containsIgnoreCase(synonyms, name)) {
                name = synonyms[0];
                break;
            }
        }
        return name;
    }

    /**
     * Determines whether or not the given string array contains the given string, ignoring case.
     *
     * @param array the list of strings.
     * @param string the string to look for.
     * @return true if the array contains the string.
     */
    private boolean containsIgnoreCase(String[] array, String string) {
        boolean containsString = false;
        for (String currentString : array) {
            if (StringUtils.equalsIgnoreCase(currentString, string)) {
                containsString = true;
                break;
            }
        }
        return containsString;
    }

    /**
     * Pulls the default value or value stated as the default from the JSON object.
     *
     * @param json the JSON object (collection of keys & values)
     * @return the value for the default value for the property.
     * @throws JSONException
     */
    private String extractDefaultValue(JSONObject json) throws JSONException {
        String key;
        if (json.has("default_value")) {
            key = json.getString("default_value");
        }
        else if (json.has("defaultvalue")) {
            key = json.getString("defaultvalue");
        }
        else {
            key = json.optString("value", null);
        }
        return key;
    }

    /**
     * Gets the data format for the input or output object being specified. If the data format name is specified then
     * the data format with the given name is used if it exists. If the data format name is not specified then the
     * "unspecified" data format is used.
     *
     * @param name the data format name.
     * @return the data format.
     */
    private DataFormat getDataFormat(String name) {
        return StringUtils.isBlank(name) ? getNamedDataFormat(UNSPECIFIED_DATA_FORMAT) : getNamedDataFormat(name);
    }

    /**
     * Gets the data format object with the given name.
     *
     * @param name the name of the data format object.
     * @return the data format.
     * @throws WorkflowException if the data format with the given name is not found.
     */
    private DataFormat getNamedDataFormat(String name) throws WorkflowException {
        DataFormat dataFormat = daoFactory.getDataFormatDao().findByName(name);
        return dataFormat == null ? getNamedDataFormat(UNSPECIFIED_DATA_FORMAT) : dataFormat;
    }

    /**
     * Finds the information type with the given name.
     *
     * @param name the information type name.
     * @return the information type.
     *         TODO: throw a WorkflowException for an unknown info type when info types are fully supported.
     */
    private InfoType getInfoType(String name) throws WorkflowException {
        return daoFactory.getInfoTypeDao().findUniqueInstanceByName(name);
    }

    /**
     * Adds old-style inputs to the list of inputs.
     *
     * @param inputs the list of inputs.
     * @param inputJsonArray the JSON array representing the old-style inputs.
     * @throws JSONException if the input JSON doesn't meet expectations.
     */
    private void addOldStyleInputs(List<DataObject> inputs, JSONArray inputJsonArray) throws JSONException {
        if (inputJsonArray != null) {
            for (int i = 0; i < inputJsonArray.length(); i++) {
                JSONObject inputJson = inputJsonArray.getJSONObject(i);
                inputs.add(inputFromJson(inputJson, inputs.size(), inputJson.getString("id")));
            }
        }
    }

    /**
     * Adds old-style outputs to the list of outputs.
     *
     * @param outputs the list of outputs.
     * @param outputJsonArray the JSON array representing the old-style outputs.
     * @throws JSONException if the output JSON doesn't meet expectations.
     */
    private void addOldStyleOutputs(List<DataObject> outputs, JSONArray outputJsonArray) throws JSONException {
        if (outputJsonArray != null) {
            for (int i = 0; i < outputJsonArray.length(); i++) {
                JSONObject outputJson = outputJsonArray.getJSONObject(i);
                outputs.add(outputFromJson(outputJson, outputs.size(), outputJson.getString("id")));
            }
        }
    }
}
