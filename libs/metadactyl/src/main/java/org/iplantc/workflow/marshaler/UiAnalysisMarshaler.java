package org.iplantc.workflow.marshaler;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.TemplateNotFoundException;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.Rule;
import org.iplantc.workflow.model.RuleType;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.model.Validator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marshals analyses for the Discovery Environment UI.
 */
public class UiAnalysisMarshaler {

    private final DaoFactory daoFactory;

    private HeterogeneousRegistry registry = new NullHeterogeneousRegistry();

    public void setRegistry(HeterogeneousRegistry registry) {
        this.registry = registry == null ? new NullHeterogeneousRegistry() : registry;
    }

    public UiAnalysisMarshaler(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    public JSONObject marshal(final TransformationActivity analysis) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", analysis.getId());
        json.put("name", analysis.getName());
        json.put("label", analysis.getName());
        json.put("type", analysis.getType());
        json.put("disabled", analysis.isDisabled());
        JsonUtils.putIfNotEmpty(json, "groups", marshalAnalysisPropertyGroups(analysis));
        return json;
    }

    // TODO: get this working with the placeholder values.
    private JSONArray marshalAnalysisPropertyGroups(final TransformationActivity analysis) throws JSONException {
        JSONArray result = new JSONArray();
        int stepNumber = 0;
        for (TransformationStep step : analysis.getSteps()) {
            stepNumber++;
            if (step.getTemplateId() != null) {
                marshalStepPropertyGroups(result, analysis, step, stepNumber);
            }
        }
        return result;
    }

    private void marshalStepPropertyGroups(JSONArray result, TransformationActivity analysis, TransformationStep step,
            int stepNumber) throws JSONException {
        Template template = loadTemplate(step.getTemplateId());
        String groupNamePrefix = getGroupNamePrefixForStep(analysis, step);
        for (PropertyGroup group : template.getPropertyGroups()) {
            JsonUtils.putIfNotNull(result, marshalPropertyGroup(analysis, step, group, groupNamePrefix, stepNumber));
        }
    }

    private JSONObject marshalPropertyGroup(TransformationActivity analysis, TransformationStep step,
            PropertyGroup group, String groupNamePrefix, int stepNumber) throws JSONException {
        JSONObject json = null;
        JSONArray props = marshalProperties(analysis, step, group.getProperties());
        if (props.length() != 0) {
            json = new JSONObject();
            json.put("id", group.getId());
            json.put("name", groupNamePrefix + group.getName());
            json.put("label", groupNamePrefix + group.getLabel());
            json.put("type", group.getGroupType());
            json.put("properties", props);
            json.put("step_number", stepNumber);
        }
        return json;
    }

    private JSONArray marshalProperties(TransformationActivity analysis, TransformationStep step,
            List<Property> properties) throws JSONException {
        JSONArray result = new JSONArray();
        for (Property prop : properties) {
            String propType = prop.getPropertyTypeName();
            if (propType.equalsIgnoreCase("output")) {
                marshalOutputProperty(result, analysis, step, prop);
            }
            else if (propType.equalsIgnoreCase("input")) {
                marshalInputProperty(result, analysis, step, prop);
            }
            else {
                marshalProperty(result, analysis, step, prop);
            }
        }
        return result;
    }

    private void marshalOutputProperty(JSONArray result, TransformationActivity analysis, TransformationStep step,
            Property prop) throws JSONException {
        if (outputPropertyVisible(analysis, step, prop)) {
            JSONObject json = new JSONObject();
            json.put("id", step.getName() + "_" + prop.getId());
            json.put("name", prop.getName());
            json.put("label", prop.getLabel());
            json.put("isVisible", prop.getIsVisible());
            JsonUtils.putIfNotNull(json, "value", prop.getDefaultValue());
            JsonUtils.putIfNotNull(json, "type", prop.getOutputTypeName());
            json.put("description", StringUtils.defaultString(prop.getDescription()));
            json.put("validator", marshalOutputPropertyValidator(prop.getDataObject()));
            result.put(json);
        }
    }

    private JSONObject marshalOutputPropertyValidator(DataObject output) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", "");
        json.put("label", "");
        json.put("required", output.isRequired());
        return json;
    }

    private void marshalInputProperty(JSONArray result, TransformationActivity analysis, TransformationStep step,
            Property prop) throws JSONException {
        if (inputPropertyVisible(analysis, step, prop)) {
            DataObject input = prop.getDataObject();
            result.put(UiInputPropertyMarshaler.instance(daoFactory, input).marshalInputProperty(step, prop, input));
        }
    }

    private void marshalProperty(JSONArray result, TransformationActivity analysis, TransformationStep step,
            Property prop) throws JSONException {
        if (propertyVisible(analysis, step, prop)) {
            JSONObject json = new JSONObject();
            json.put("id", step.getName() + "_" + prop.getId());
            json.put("name", hasMustContainRule(prop) ? "" : prop.getName());
            json.put("label", prop.getLabel());
            json.put("isVisible", prop.getIsVisible());
            JsonUtils.putIfNotNull(json, "value", prop.getDefaultValue());
            JsonUtils.putIfNotNull(json, "type", prop.getPropertyTypeName());
            json.put("description", StringUtils.defaultString(prop.getDescription()));
            JsonUtils.putIfNotNull(json, "validator", marshalValidator(step.getName(), prop.getValidator()));
            result.put(json);
        }
    }

    private boolean hasMustContainRule(Property prop) {
        Validator validator = prop.getValidator();
        if (validator != null) {
            for (Rule rule : validator.getRules()) {
                if (rule.getRuleType().getName().equals("MustContain")) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean outputPropertyVisible(TransformationActivity analysis, TransformationStep step, Property prop) {
        return !propertyVisible(analysis, step, prop)                                   ? false
             : step.getTransformation().containsProperty(prop.getDataObject().getId())  ? false
             : analysis.isTargetInMapping(step.getName(), prop.getDataObject().getId()) ? false
             : prop.getDataObject().isImplicit()                                        ? false
             :                                                                            true;
    }

    private boolean inputPropertyVisible(TransformationActivity analysis, TransformationStep step, Property prop) {
        return !propertyVisible(analysis, step, prop)                                   ? false
             : step.getTransformation().containsProperty(prop.getDataObject().getId())  ? false
             : analysis.isTargetInMapping(step.getName(), prop.getDataObject().getId()) ? false
             :                                                                            true;
    }

    private boolean propertyVisible(TransformationActivity analysis, TransformationStep step, Property prop) {
        return step.getTransformation().containsProperty(prop.getId()) ? false
             : !prop.getIsVisible()                                    ? false
             :                                                           true;
    }

    private JSONObject marshalValidator(String stepName, Validator validator) throws JSONException {
        JSONObject json = null;
        if (validator != null) {
            json = new JSONObject();
            json.put("id", validator.getId());
            json.put("name", validator.getName());
            json.put("label", validator.getLabel());
            json.put("required", validator.isRequired());
            JsonUtils.putIfNotNull(json, "rules", marshalRules(stepName, validator.getRules()));
        }
        return json;
    }

    private JSONArray marshalRules(String stepName, List<Rule> rules) throws JSONException {
        JSONArray result = null;
        if (!rules.isEmpty()) {
            result = new JSONArray();
            for (Rule rule : rules) {
                result.put(marshalRule(stepName, rule));
            }
        }
        return result;
    }

    private JSONObject marshalRule(String stepName, Rule rule) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(getRuleTypeName(rule), marshalRuleArguments(rule));
        return json;
    }

    private String getRuleTypeName(Rule rule) {
        RuleType ruleType = rule.getRuleType();
        if (ruleType == null || StringUtils.isBlank(ruleType.getName())) {
            throw new WorkflowException("rule with no type encountered");
        }
        return ruleType.getName();
    }

    private JSONArray marshalRuleArguments(Rule rule) throws JSONException {
        JSONArray result = new JSONArray();
        for (String arg : rule.getArguments()) {
            result.put(convertRuleArgument(arg));
        }
        return result;
    }

    private Object convertRuleArgument(String arg) throws JSONException {
        String trimmedArg = arg.trim();
        JSONObject jsonArg = asJsonObject(trimmedArg);
        return jsonArg != null ? jsonArg
                : trimmedArg.matches("[-]?\\p{Digit}+") ? new Integer(trimmedArg)
                : trimmedArg.matches("[-]?\\p{Digit}+[\\.]\\p{Digit}+") ? new Double(trimmedArg)
                : arg;
    }

    private JSONObject asJsonObject(String arg) {
        if (arg.startsWith("{") && arg.endsWith("}")) {
            try {
                JSONObject json = new JSONObject(arg);
                if (!json.has("name")) {
                    json.put("name", "");
                }
                return json;
            }
            catch (JSONException ignore) {
                return null;
            }
        }
        return null;
    }

    private Template loadTemplate(String id) {
        Template template = loadTemplateFromRegistry(id);
        if (template == null) {
            template = loadTemplateFromDatabase(id);
        }
        return template;
    }

    private Template loadTemplateFromRegistry(String id) {
        return registry.get(Template.class, id);
    }

    private Template loadTemplateFromDatabase(String id) {
        Template template = daoFactory.getTemplateDao().findById(id);
        if (template == null) {
            throw new TemplateNotFoundException(id);
        }
        return template;
    }

    private String getGroupNamePrefixForStep(TransformationActivity analysis, TransformationStep step) {
        String prefix = "";
        if (analysis.isMultistep()) {
            Template template = daoFactory.getTemplateDao().findById(step.getTemplateId());
            if (template != null) {
                prefix = template.getName() + " - ";
            }
        }
        return prefix;
    }
}
