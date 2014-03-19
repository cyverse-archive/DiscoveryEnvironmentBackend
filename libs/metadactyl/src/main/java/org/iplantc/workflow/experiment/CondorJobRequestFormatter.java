package org.iplantc.workflow.experiment;

import static org.iplantc.workflow.experiment.ParamUtils.setParamNameAndValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.experiment.dto.JobConstructor;
import org.iplantc.workflow.experiment.files.FileResolver;
import org.iplantc.workflow.experiment.files.FileResolverFactory;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.user.UserDetails;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;
import org.iplantc.workflow.util.SfJsonUtils;

/**
 * Formats a submission request for a job that will be executed on Condor. The code in this class was mostly extracted
 * from ExperimentRunner and only minor refactoring work was done.  Note: this class not thread safe.
 */
public class CondorJobRequestFormatter implements JobRequestFormatter {

    private static final int JSON_INDENT = 4;

    private static final String CONDOR_TYPE = "condor";

    private static final Logger LOG = Logger.getLogger(CondorJobRequestFormatter.class);

    private static final Pattern FILE_URL_PATTERN = Pattern.compile("^(?:file://|/)");

    private static final String[] IGNORED_PROPERTY_TYPE_NAMES = {"EnvironmentVariable"};

    private static final Set<String> IGNORED_PROPERTY_TYPES
            = new HashSet<String>(Arrays.asList(IGNORED_PROPERTY_TYPE_NAMES));

    private final DaoFactory daoFactory;

    private final UrlAssembler urlAssembler;

    private final UserDetails userDetails;

    private final JSONObject experiment;

    private final boolean debug;

    private final FileResolverFactory fileResolverFactory;

    private String stdoutFilename;

    private String stderrFilename;

    private final Map<String, String> outputPropertyValues = new HashMap<String, String>();

    public CondorJobRequestFormatter(DaoFactory daoFactory, UrlAssembler urlAssembler,
            UserDetails userDetails, JSONObject experiment) {
        this.daoFactory = daoFactory;
        this.urlAssembler = urlAssembler;
        this.userDetails = userDetails;
        this.experiment = experiment;
        this.debug = experiment.optBoolean("debug", false);
        this.fileResolverFactory = new FileResolverFactory(daoFactory);
    }

    @Override
    public JSONObject formatJobRequest() {
        JobConstructor jobConstructor = new JobConstructor("submit", CONDOR_TYPE);
        jobConstructor.setExperimentJson(experiment);

        String analysisId = experiment.getString("analysis_id");
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis " + analysisId + " not found");
        }

        jobConstructor.setAnalysis(analysis);

        long workspaceId = Long.parseLong(experiment.getString("workspace_id"));
        LOG.debug("Workspace Id: " + workspaceId);

        jobConstructor.setUsername(userDetails.getShortUsername());

        JSONObject job = jobConstructor.getJob().toJson();

        job.put("email", userDetails.getEmail());

        JSONObject config = experiment.getJSONObject("config");

        List<TransformationStep> steps = analysis.getSteps();

        JSONArray stepArray = new JSONArray();

        for (TransformationStep currentStep : steps) {
            JSONObject step1 = new JSONObject();

            stderrFilename = null;
            stdoutFilename = null;

            step1.put("name", currentStep.getName());
            step1.put("type", CONDOR_TYPE);

            Transformation transformation = currentStep.getTransformation();

            Template template = daoFactory.getTemplateDao().findById(transformation.getTemplate_id());

            JSONObject finalConfig = new JSONObject();

            JSONArray jinputs = new JSONArray();
            JSONArray params = new JSONArray();

            // Format inputs and properties for inputs that are not referenced by other properties.
            formatInputs(template, currentStep, config, jinputs);
            formatUnreferencedInputProperties(template, currentStep, config, params, transformation,
                    analysis, stepArray);

            // Format the properties.
            formatProperties(analysis, template, currentStep, transformation, params, config, stepArray);

            // Format the environment-variable settings.
            CondorEnvironmentVariableFormatter envFormatter
                    = new CondorEnvironmentVariableFormatter(template, currentStep.getName(), transformation, config);
            step1.put("environment", envFormatter.format());

            // Format outputs and properties for outputs taht are not referenced by other properties.
            JSONArray outputs_section = new JSONArray();
            formatOutputs(template, outputs_section);
            formatUnreferencedOutputProperties(template, transformation, params);

            finalConfig.put("input", jinputs);
            finalConfig.put("params", params);
            finalConfig.put("output", outputs_section);
            step1.put("config", finalConfig);

            /**
             * retrieve component for template *
             */
            String componentId = template.getComponent();
            step1.put("component", new DeployedComponentFormatter(daoFactory).formatComponent(componentId));

            // Add the output redirections if there are any.
            if (stderrFilename != null) {
                step1.put("stderr", stderrFilename);
            }
            if (stdoutFilename != null) {
                step1.put("stdout", stdoutFilename);
            }

            /**
             * assemble the job JSON request *
             */
            stepArray.add(step1);

        }

        job.put("steps", stepArray);
        LOG.debug("Job: " + job);
        return job;
    }

    private void formatUnreferencedOutputProperties(Template template, Transformation transformation, JSONArray params) {
        for (DataObject outputObject : template.findUnreferencedOutputs()) {
            int order = getDataObjectOrder(outputObject);
            if (order < 0) {
                continue;
            }

            String value = transformation.containsProperty(outputObject.getId())
                    ? transformation.getValueForProperty(CONDOR_TYPE)
                    : outputObject.getName();

            if (!StringUtils.isBlank(value)) {
                JSONObject param = new JSONObject();
                setParamNameAndValue(param, outputObject.getSwitchString(), value);
                param.put("order", order);
                param.put("id", outputObject.getId());
                params.add(param);
                updateRedirectionFilenames(outputObject, value);
            }
        }
    }

    private void updateRedirectionFilenames(DataObject dataObject, String filename) {
        String dataSourceName = dataObject.getDataSourceName();
        if (StringUtils.equals(dataSourceName, "stdout")) {
            stdoutFilename = filename;
        }
        else if (StringUtils.equals(dataSourceName, "stderr")) {
            stderrFilename = filename;
        }
    }

    private void formatOutputs(Template template, JSONArray outputs_section) {
        formatDefinedOutputs(template, outputs_section);
        formatLogOutput(outputs_section);
    }

    private void formatDefinedOutputs(Template template, JSONArray outputs_section) {
        for (DataObject outputObject : template.getOutputs()) {

            String id = outputObject.getId();
            String value = outputPropertyValues.containsKey(id) ? outputPropertyValues.get(id) : outputObject.getName();

            if (!StringUtils.isBlank(value)) {
                JSONObject out = new JSONObject();
                out.put("name", value);
                out.put("property", value);
                out.put("type", outputObject.getInfoTypeName());
                out.put("multiplicity", outputObject.getMultiplicityName());
                out.put("retain", debug || outputObject.getRetain());
                outputs_section.add(out);
                updateRedirectionFilenames(outputObject, value);
            }
        }
    }

    private void formatLogOutput(JSONArray outputs_section) {
        JSONObject out = new JSONObject();
        out.put("name", "logs");
        out.put("property", "logs");
        out.put("type", "File");
        out.put("multiplicity", "collection");
        out.put("retain", true);
        outputs_section.add(out);
    }

    private void formatProperties(TransformationActivity analysis, Template template, TransformationStep currentStep,
            Transformation transformation, JSONArray params, JSONObject config, JSONArray stepArray)
            throws NumberFormatException {

        String stepName = currentStep.getName();
        for (PropertyGroup group : template.getPropertyGroups()) {
            List<Property> properties = group.getProperties();

            for (Property p : properties) {
                if (p.getPropertyType().getName().equals("Info")) {
                    continue;
                }

                String key = stepName + "_" + p.getId();
                if (transformation.containsProperty(p.getId())) {
                    params.add(formatPropertyFromTransformation(p, transformation));

                }
                else if (SfJsonUtils.contains(config, key)) {
                    params.addAll(buildParamsForProperty(p, SfJsonUtils.defaultString(config, key), stepName));
                }
                else if (!p.getIsVisible()) {
                    params.addAll(buildParamsForProperty(p, getDefaultValue(p), stepName));
                }
                else if (p.getDataObject() != null && analysis.isTargetInMapping(currentStep.getName(), p.getId())) {
                    formatMappedInput(analysis, currentStep, p.getDataObject(), stepArray, params);
                }
            }
        }
    }

    private void formatUnreferencedInputProperties(Template template, TransformationStep currentStep, JSONObject config,
            JSONArray params, Transformation transformation, TransformationActivity analysis, JSONArray stepArray) {
        for (DataObject currentInput : template.findUnreferencedInputs()) {
            // this is temporary - we're skipping the resolution of
            // any input DataObject of type "ReconcileTaxa" because
            // the resolution is not implemented yet... (lenards)
            if (currentInput.getInfoTypeName().equalsIgnoreCase("reconciletaxa")) {
                continue;
            }

            String key = currentStep.getName() + "_" + currentInput.getId();
            if (SfJsonUtils.contains(config, key)) {
                String path = config.getString(key);
                if (!StringUtils.isBlank(path)) {
                    JSONArray objects = getInputJSONObjects(currentInput, path);
                    addParameterDefinitionsForDataObject(currentStep.getName(), params, currentInput, objects);
                }
            }
            else if (transformation.containsProperty(currentInput.getId())) {
                JSONObject prop = new JSONObject();
                String value = transformation.getValueForProperty(currentInput.getName());
                setParamNameAndValue(prop, currentInput.getSwitchString(), value);
                prop.put("order", getDataObjectOrder(currentInput));
                prop.put("id", currentInput.getId());
                params.add(prop);
            }
            else if (analysis.isTargetInMapping(currentStep.getName(), currentInput.getId())) {
                formatMappedInput(analysis, currentStep, currentInput, stepArray, params);
            }
        }
    }

    private void formatMappedInput(TransformationActivity analysis, TransformationStep currentStep,
            DataObject currentInput, JSONArray stepArray, JSONArray params) {
        ArrayList<InputOutputMap> maps = analysis.getMappingsForTargetStep(currentStep.getName());
        LOG.debug("is target: " + currentInput.getId());
        for (InputOutputMap map : maps) {
            TransformationStep source = map.getSource();
            JSONObject jsonSource = retrieveJSONStep(stepArray, source.getName());
            Map<String, String> relation = map.getInput_output_relation();
            for (String sourceObject : relation.keySet()) {
                LOG.debug("Source object: " + sourceObject);
                if (relation.get(sourceObject).equals(currentInput.getId())) {
                    JSONObject prop = new JSONObject();
                    String value = retrieveValueForProperty(sourceObject, source, jsonSource);
                    setParamNameAndValue(prop, currentInput.getSwitchString(), value);
                    prop.put("order", getDataObjectOrder(currentInput));
                    prop.put("id", currentInput.getId());
                    params.add(prop);
                }
            }
        }
    }

    private void formatInputs(Template template, TransformationStep currentStep, JSONObject config, JSONArray jinputs) {
        for (DataObject currentInput : template.getInputs()) {
            // this is temporary - we're skipping the resolution of
            // any input DataObject of type "ReconcileTaxa" because
            // the resolution is not implemented yet... (lenards)
            if (currentInput.getInfoTypeName().equalsIgnoreCase("reconciletaxa")) {
                continue;
            }
            String key = currentStep.getName() + "_" + currentInput.getId();
            if (SfJsonUtils.contains(config, key)) {
                String path = config.getString(key);
                if (!StringUtils.isBlank(path)) {
                    jinputs.addAll(getInputJSONObjects(currentInput, path));
                }
            }
        }
    }

    /**
     * Formats a property using the value contained in the transformation. Note that the property's omit-if-blank
     * setting is ignored for properties whose values are obtained from the transformation. This was done to maintain
     * backward compatibility.
     *
     * @param property the property.
     * @param transformation the transformation.
     * @return the formatted parameter.
     */
    private JSONObject formatPropertyFromTransformation(Property property, Transformation transformation) {
        JSONObject jprop = new JSONObject();
        setParamNameAndValue(jprop, property.getName(), transformation.getValueForProperty(property.getId()));
        jprop.put("order", getPropertyOrder(property));
        jprop.put("id", property.getId());
        return jprop;
    }

    private JSONArray getInputJSONObjects(DataObject input, String path) {
        JSONArray result = new JSONArray();

        logDataObject("input", input);
        FileResolver fileResolver = fileResolverFactory.getFileResolver(input.getInfoTypeName());
        if (fileResolver != null) {
            JSONObject inputJson = createInputJsonForResolvedFile(extractInputName(path), input, fileResolver);
            if (inputJson != null) {
                result.add(inputJson);
            }
        }
        else {
            if (!input.getMultiplicityName().equals("many")) {
                result.add(createInputJson(path, input));
            }
            else {
                JSONArray jsonFiles = ParamUtils.jsonArrayFromString(path);
                if (jsonFiles != null) {
                    for (int i = 0, pathCount = jsonFiles.size(); i < pathCount; i++) {
                        String currentPath = jsonFiles.getString(i);
                        result.add(createInputJson(currentPath, input));
                    }
                }
            }
        }

        return result;
    }

    private String extractInputName(String path) {
        JSONObject json = jsonObjectFromString(path);
        return json == null ? path : SfJsonUtils.optString(json, "", "uuid", "name");
    }

    private JSONObject jsonObjectFromString(String json) {
        try {
            return (JSONObject) JSONSerializer.toJSON(json);
        }
        catch (Exception ignore) {
            return null;
        }
    }

    private void logDataObject(String label, DataObject input) {
        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug(label + ": " + input.toJson().toString(JSON_INDENT));
            }
            catch (Exception ignore) {
            }
        }
    }

    private JSONObject createInputJsonForResolvedFile(String name, DataObject input, FileResolver fileResolver) {
        JSONObject result = null;
        String url = fileResolver.getFileAccessUrl(name);
        if (!StringUtils.isBlank(url) && !isFileUrl(url)) {
            result = new JSONObject();
            result.put("name", name);
            result.put("property", name);
            result.put("type", input.getInfoTypeName().trim());
            result.put("value", url);
            result.put("id", input.getId());
            result.put("retain", debug || input.getRetain());
        }
        return result;
    }

    private boolean isFileUrl(String url) {
        return FILE_URL_PATTERN.matcher(url).find();
    }

    private JSONObject createInputJson(String path, DataObject input) {
        JSONObject in = new JSONObject();
        String filename = basename(path);
        in.put("name", filename);
        in.put("property", filename);
        in.put("type", input.getInfoTypeName().trim());
        in.put("value", urlAssembler.assembleUrl(path));
        in.put("id", input.getId());
        in.put("multiplicity", input.getMultiplicityName());
        in.put("retain", debug || input.getRetain());
        return in;
    }

    private String basename(String path) {
        int slashpos = path.lastIndexOf("/");
        return slashpos == -1 ? path : path.substring(slashpos + 1);
    }

    private void addParameterDefinitionsForDataObject(String stepName, JSONArray params, DataObject dataObject,
            JSONArray dataInfo) {
        List<String> paths = getPathsForDataObject(stepName, dataObject, dataInfo);
        for (String path : paths) {
            params.add(getParameterDefinitionForDataObject(dataObject, path));
        }
    }

    private List<String> getPathsForDataObject(String stepName, DataObject dataObject, JSONArray dataInfo) {
        List<String> paths = new ArrayList<String>();
        if (dataObject.getMultiplicityName().equals("single")) {
            paths.addAll(getPathsForSingleInput(stepName, dataObject, dataInfo));
        }
        else {
            paths.addAll(getPathsForMultipleInputs(dataInfo));
        }
        return paths;
    }

    private List<String> getPathsForMultipleInputs(JSONArray dataInfo) {
        List<String> paths = new ArrayList<String>();
        for (int i = 0; i < dataInfo.size(); i++) {
            paths.add(dataInfo.getJSONObject(i).getString("property"));
        }
        return paths;
    }

    /**
     * Gets the list of paths to use for a single input file, which may be empty. This is brittle, but we need to treat
     * reference genomes as a special case because an input object is not created for reference genomes. I'm afraid a
     * complete refactoring of this class would be required to find a better solution, though.
     *
     * @param stepName the name of the current transformation step.
     * @param input the data object representing the input object.
     * @param dataInfo the list of input objects.
     * @return the list of paths to use for the input file, which may contain zero elements or one element.
     */
    private List<String> getPathsForSingleInput(String stepName, DataObject input, JSONArray dataInfo) {
        List<String> result = new ArrayList<String>();
        FileResolver fileResolver = fileResolverFactory.getFileResolver(input.getInfoTypeName());
        if (fileResolver != null) {
            String key = stepName + "_" + input.getId();
            String propertyValue = experiment.getJSONObject("config").getString(key);
            String resolvedPath = fileResolver.getFileAccessUrl(extractInputName(propertyValue));
            if (!StringUtils.isBlank(resolvedPath)) {
                result.add(resolvedPath);
            }
        }
        else if (dataInfo.size() > 0) {
            result.add(dataInfo.getJSONObject(0).getString("property"));
        }
        return result;
    }

    /**
     * Returns an array of objects representing the given input objects
     *
     * @param dataObject the data object for which the param needs to be retrieved
     * @param path the relative path to the file.
     */
    private JSONObject getParameterDefinitionForDataObject(DataObject dataObject, String path) {
        JSONObject parameter = new JSONObject();
        setParamNameAndValue(parameter, dataObject.getSwitchString(), path);
        parameter.put("order", getDataObjectOrder(dataObject));
        parameter.put("id", dataObject.getId());
        return parameter;
    }

    protected int getDataObjectOrder(DataObject dataObject) {
        int order = dataObject.getOrderd();
        if (order < 0 && !StringUtils.isBlank(dataObject.getSwitchString())) {
            order = 0;
        }
        return order;
    }

    private String retrieveValueForProperty(String property, TransformationStep step, JSONObject jstep) {

        Transformation transformation = step.getTransformation();

        String originalName = property.replace("in#", "").replace(step.getName() + "_", "");

        if (property.contains("in#")) {

            JSONObject jsonInput = getJSONProperty(jstep, originalName);
            if (jsonInput != null) {
                return jsonInput.getString("value");
            }
            else {
                throw new WorkflowException("A value for property " + step.getName() + "_" + originalName
                        + " needs to be input in order to be used in a mapping.");
            }
        }

        String userInput = outputPropertyValues.get(originalName);
        if (!StringUtils.isEmpty(userInput)) {
            return userInput;
        }

        if (transformation.containsProperty(originalName)) {
            return transformation.getPropertyValues().get(originalName);
        }
        else {
            String templateId = transformation.getTemplate_id();
            Template template = daoFactory.getTemplateDao().findById(templateId);
            if (template == null) {
                throw new WorkflowException("template " + templateId + " not found");
            }
            return getPropertyName(originalName, template);
        }
    }

    protected String getPropertyName(String originalName, Template template) {
        String retval;
        try {
            retval = template.getOutputName(originalName);
        }
        catch (Exception e) {
            throw new WorkflowException("unable to determine the output name for " + originalName, e);
        }
        return retval;
    }

    public JSONObject getJSONProperty(JSONObject step, String name) {
        JSONObject config = step.getJSONObject("config");
        JSONArray inputs = config.getJSONArray("input");

        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.getJSONObject(i).getString("id").equals(name)) {
                return inputs.getJSONObject(i);
            }
        }

        JSONArray params = config.getJSONArray("params");

        for (int i = 0; i < params.size(); i++) {
            if (params.getJSONObject(i).getString("id").equals(name)) {
                return params.getJSONObject(i);
            }
        }

        return null;
    }

    public JSONObject retrieveJSONStep(JSONArray steps, String step_name) {

        for (int i = 0; i < steps.size(); i++) {
            if (steps.getJSONObject(i).getString("name").equals(step_name)) {
                return steps.getJSONObject(i);
            }
        }
        return null;
    }

    private List<JSONObject> buildParamsForProperty(Property property, String value, String stepName) {
        List<JSONObject> jprops = new ArrayList<JSONObject>();
        String propertyTypeName = property.getPropertyTypeName();
        if (StringUtils.equals(propertyTypeName, "TreeSelection")) {
            jprops.addAll(formatTreeSelectionProperty(property, value));
        }
        else if (StringUtils.endsWith(propertyTypeName, "Selection")) {
            CollectionUtils.addIgnoreNull(jprops, formatSelectionProperty(property, value));
        }
        else if (StringUtils.equals(propertyTypeName, "Flag")) {
            CollectionUtils.addIgnoreNull(jprops, formatFlagProperty(property, value));
        }
        else if (StringUtils.equals(propertyTypeName, "BarcodeSelector") || StringUtils.equals(propertyTypeName,
                "ClipperSelector")) {
            throw new UnsupportedPropertyTypeException(propertyTypeName);
        }
        else if (StringUtils.equals(propertyTypeName, "Input")) {
            jprops.addAll(formatInputProperties(property, value, stepName));
        }
        else if (StringUtils.equals(propertyTypeName, "Output")) {
            CollectionUtils.addIgnoreNull(jprops, formatOutputProperty(property, value));
        }
        else if (!IGNORED_PROPERTY_TYPES.contains(property.getPropertyTypeName())) {
            CollectionUtils.addIgnoreNull(jprops, formatDefaultProperty(property, value));
        }

        return removePropertiesWithNegativeOrdering(jprops);
    }

    /**
     * Removes any properties that still have a negative ordering from a list of properties.
     *
     * @param props the list of properties.
     * @return the filtered list of properties.
     */
    private List<JSONObject> removePropertiesWithNegativeOrdering(List<JSONObject> props) {
        return ListUtils.filter(new Predicate<JSONObject>() {

            @Override
            public Boolean call(JSONObject arg) {
                return arg.optInt("order", -1) >= 0;
            }
        }, props);
    }

    protected JSONObject formatOutputProperty(Property property, String value) {
        DataObject output = property.getDataObject();
        if (output.isImplicit()) {
            return null;
        }
        outputPropertyValues.put(output.getId(), value);
        return StringUtils.equals(output.getDataSourceName(), "file") ? formatDefaultProperty(property, value) : null;
    }

    protected JSONObject formatDefaultProperty(Property property, String value) {
        JSONObject jprop = null;
        if (!property.getOmitIfBlank() || !StringUtils.isBlank(value)) {
            jprop = initialPropertyJson(property);
            setParamNameAndValue(jprop, property.getName(), value);
            jprop.put("id", property.getId());
        }
        return jprop;
    }

    /**
     * Formats a flag property. Note that this method ignores the property's omit-if-blank setting, which isn't required
     * for flag properties, which are always omitted if the option selected by the user corresponds to a missing
     * command-line flag.
     *
     * @param property the property being formatted.
     * @param value the property value.
     * @return the formatted property or null if the property should not be included on the command line.
     */
    protected JSONObject formatFlagProperty(Property property, String value) {
        JSONObject jprop = null;

        // Parse the boolean value and the list of possible values.
        boolean booleanValue = Boolean.parseBoolean(value.trim());
        String[] values = property.getName().split("\\s*,\\s*");

        // Determine which value was selected.
        int index = booleanValue ? 0 : 1;
        String selectedValue = values.length > index ? values[index] : null;

        // Format the property only if a value was selected.
        if (selectedValue != null && !StringUtils.isBlank(selectedValue)) {
            String[] components = selectedValue.split("\\s+|(?<==)", 2);
            jprop = new JSONObject();
            jprop.put("id", property.getId());
            setParamNameAndValue(jprop, components[0], components.length > 1 ? components[1] : "");
            jprop.put("order", property.getOrder());
        }

        return jprop;
    }

    /**
     * Formats a selection property. Note that this method ignores the property's omit-if-blank setting, which
     * isn't required for selection properties because the name and value are specified separately for each
     * selection.
     *
     * @param property the property.
     * @param arg the argument.
     * @return the formatted parameter or null if the parameter shouldn't be formatted.
     */
    private JSONObject formatSelectionProperty(Property property, String arg) {
        JSONObject jsonArg = jsonObjectFromString(arg);
        return jsonArg == null ? null : formatSelectionProperty(property, jsonArg);
    }

    /**
     * Formats a selection property. Note that this method ignores the property's omit-if-blank setting, which
     * isn't required for selection properties because the name and value are specified separately for each
     * selection.
     *
     * @param property the property.
     * @param arg the argument.
     * @return the formatted parameter or null if the parameter shouldn't be formatted.
     */
    private JSONObject formatSelectionProperty(Property property, JSONObject arg) {
        JSONObject result = null;
        String name = SfJsonUtils.defaultString(arg, "name");
        String value = SfJsonUtils.defaultString(arg, "value");
        if (!StringUtils.isEmpty(name) || !StringUtils.isEmpty(value)) {
            result = initialPropertyJson(property);
            result.put("id", property.getId());
            setParamNameAndValue(result, name, value);
        }
        return result;
    }

    private List<JSONObject> formatTreeSelectionProperty(Property property, String values) {
        List<JSONObject> params = new ArrayList<JSONObject>();
        JSONArray args = ParamUtils.jsonArrayFromString(values);

        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                JSONObject result = formatSelectionProperty(property, args.getJSONObject(i));
                CollectionUtils.addIgnoreNull(params, result);
            }
        }

        return params;
    }

    private JSONObject initialPropertyJson(Property property) {
        JSONObject json = new JSONObject();
        json.put("order", getPropertyOrder(property));
        return json;
    }

    protected int getPropertyOrder(Property property) {
        int order = property.getOrder();
        if (order < 0 && !StringUtils.isBlank(property.getName())) {
            order = 0;
        }
        return order;
    }

    private List<JSONObject> formatInputProperties(Property property, String value, String stepName) {
        List<JSONObject> params = new ArrayList<JSONObject>();
        JSONArray objects = getInputJSONObjects(property.getDataObject(), value);
        List<String> paths = getPathsForDataObject(stepName, property.getDataObject(), objects);
        for (String path : paths) {
            if (!property.getOmitIfBlank() || !StringUtils.isBlank(path)) {
                params.add(getParameterDefinitionForDataObject(property.getDataObject(), path));
            }
        }
        return params;
    }

    private String getDefaultValue(Property property) {
        String type = property.getPropertyTypeName();
        return type.equalsIgnoreCase("output") ? property.getDataObject().getName() : property.getDefaultValue();
    }
}
