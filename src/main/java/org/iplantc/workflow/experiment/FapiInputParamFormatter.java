package org.iplantc.workflow.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.model.Template;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.util.SfJsonUtils;

import static org.iplantc.workflow.experiment.ParamUtils.setParamNameAndValue;

/**
 * Formats input parameters for jobs that are submitted to the Foundational API.
 * 
 * @author Dennis Roberts
 */
public class FapiInputParamFormatter {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * The analysis that is currently being formatted.
     */
    private TransformationActivity analysis;

    /**
     * The transformation step that is currently being formatted.
     */
    private TransformationStep step;

    /**
     * The experiment configuration.
     */
    private JSONObject config;

    /**
     * A map of property names to property values.
     */
    private Map<String, List<String>> propertyValues;

    /**
     * @param daoFactory used to obtain data access objects.
     * @param analysis the analysis that is currently being formatted.
     * @param step the transformation step that is currently being formatted.
     * @param config the experiment configuration.
     * @param propertyValues a map of property names to property values.
     */
    public FapiInputParamFormatter(DaoFactory daoFactory, TransformationActivity analysis, TransformationStep step,
        JSONObject config, Map<String, List<String>> propertyValues)
    {
        this.daoFactory = daoFactory;
        this.analysis = analysis;
        this.step = step;
        this.config = config;
        this.propertyValues = propertyValues;
    }

    /**
     * Adds the parameters for the given input.
     * 
     * @param params the list of parameters.
     * @param input the input.
     */
    public void addParamsForInput(JSONArray params, DataObject input) {
        List<String> specifiedFiles = getSpecifiedFiles(input);
        if (specifiedFiles != null && input.getOrderd() >= 0) {
            addParamsForSpecifiedFiles(params, input, specifiedFiles);
        }
    }

    /**
     * Gets the string representing the list of specified files for the given input.
     * 
     * @param input the data object representing the input file.
     * @return the string representing the list of files or null if the input wasn't specified by the user.
     */
    private List<String> getSpecifiedFiles(DataObject input) {
        List<String> files = null;
        String key = step.getName() + "_" + input.getId();
        if (filesSpecifiedByUser(key)) {
            files = specifiedFilesToList(input, config.getString(key));
        }
        else if (filesSpecifiedByTransformation(input)) {
            Transformation transformation = step.getTransformation();
            files =  specifiedFilesToList(input, transformation.getValueForProperty(input.getId()));
        }
        else if (analysis.isTargetInMapping(step.getName(), input.getId())) {
            files = getSourceFileNamesFromInputOutputMappings(input);
        }
        return files;
    }

    /**
     * Gets the source file names from the input/output mappings for an analysis.
     * 
     * @param input the current input object.
     * @return the list of source file names.
     */
    private List<String> getSourceFileNamesFromInputOutputMappings(DataObject input) {
        List<String> sourceFileNames = new ArrayList<String>();
        for (InputOutputMap map : analysis.getMappingsForTargetStep(step.getName())) {
            TransformationStep source = map.getSource();
            Map<String, String> relation = map.getInput_output_relation();
            for (String sourcePropertyName : relation.keySet()) {
                String destPropertyName = relation.get(sourcePropertyName);
                if (StringUtils.equals(destPropertyName, input.getId())) {
                    sourceFileNames.addAll(findSourceFileNames(source, sourcePropertyName, destPropertyName));
                }
            }
        }
        return sourceFileNames;
    }

    /**
     * Finds the source file names for the given source transformation step, source property name and destination
     * property name.
     * 
     * @param source the source transformation step.
     * @param sourcePropertyName the name of the source property.
     * @param destPropertyName the name of the destination property.
     * @return the list of source file names.
     */
    private List<String> findSourceFileNames(TransformationStep source, String sourcePropertyName,
        String destPropertyName)
    {
        List<String> sourceFileNames = new ArrayList<String>();
        Transformation sourceTransformation = source.getTransformation();
        String originalName = sourcePropertyName.replaceAll("^in#", "").replace(source.getName() + "_", "");
        if (sourcePropertyName.startsWith("in#")) {
            sourceFileNames.addAll(getInputPropertyValuesFromSourceStep(source, originalName));
        }
        else if (sourceTransformation != null) {
            if (sourceTransformation.containsProperty(sourcePropertyName)) {
                sourceFileNames.add(sourceTransformation.getValueForProperty(sourcePropertyName));
            }
            else {
                sourceFileNames.add(getOutputNameFromTemplate(sourceTransformation.getTemplate_id(), originalName));
            }
        }
        return sourceFileNames;
    }

    /**
     * Obtains the name of the output with the given identifier from the template with the given identifier.
     * 
     * @param templateId the template identifier.
     * @param outputId the output identifier.
     * @return the name of the output.
     * @throws WorkflowException if the output name can't be determined.
     */
    private String getOutputNameFromTemplate(String templateId, String outputId) throws WorkflowException {
        Template template = findTemplate(templateId);
        String outputName = null;
        try {
            outputName = template.getOutputName(outputId);
        }
        catch (Exception e) {
            throw new WorkflowException("unable to determine the output name for " + outputId, e);
        }
        return outputName;
    }

    /**
     * Finds the template with the given identifier.
     * 
     * @param templateId the template identifier.
     * @return the template.
     * @throws WorkflowException if the template can't be found.
     */
    private Template findTemplate(String templateId) throws WorkflowException {
        Template template = daoFactory.getTemplateDao().findById(templateId);
        if (template == null) {
            throw new WorkflowException("unable to find template, " + templateId);
        }
        return template;
    }

    /**
     * Obtains the input property values from the source step.
     * 
     * @param source the source transformation step.
     * @param originalName the original name of the property.
     * @return the input property value.
     */
    private List<String> getInputPropertyValuesFromSourceStep(TransformationStep source, String originalName) {
        return propertyValues.get(source.getName() + "_" + originalName);
    }

    /**
     * Converts files specified for an input to a list.
     * 
     * @param input the current input object.
     * @param specifiedFiles the string identifying the files that have been specified.
     * @return the list of files.
     */
    @SuppressWarnings("unchecked")
    private List<String> specifiedFilesToList(DataObject input, String specifiedFiles) {
        List<String> files;
        if (input.getMultiplicityName().equals("many")) {
            files = new ArrayList<String>();
            JSONArray jsonFiles = ParamUtils.jsonArrayFromString(specifiedFiles);
            if (jsonFiles != null) {
                files.addAll(jsonFiles);
            }
        }
        else if (!StringUtils.isBlank(specifiedFiles)) {
            files = Arrays.asList(specifiedFiles);
        }
        else {
            files = new ArrayList<String>();
        }
        updatePaths(files);
        return files;
    }

    /**
     * Determines whether or not the files for the given input were specified by the current transformation.
     * 
     * @param input the data object representing the input file.
     * @return true if the files are specified by the transformation.
     */
    private boolean filesSpecifiedByTransformation(DataObject input) {
        Transformation transformation = step.getTransformation();
        return transformation != null && transformation.containsProperty(input.getId());
    }

    /**
     * Determines whether or not the file corresponding to the given key was specified by the user.
     * 
     * @param key the key used to specify the file.
     * @return true if the file was specified by the user.
     */
    protected boolean filesSpecifiedByUser(String key) {
        return config != null && !config.isNullObject() && SfJsonUtils.contains(config, key);
    }

    /**
     * Adds all of the parameters for files that were specified by the user.
     * 
     * @param params the list of parameters.
     * @param input the input being formatted.
     * @param specifiedFiles the list of specified files.
     */
    private void addParamsForSpecifiedFiles(JSONArray params, DataObject input, List<String> specifiedFiles) {
        for (String path : specifiedFiles) {
            if (!StringUtils.isBlank(path)) {
                addParamValueToPropertyMap(input.getId(), path);
                addParamForOneSpecifiedFile(params, input, path);
            }
        }
    }

    /**
     * Adds a parameter value to the property map.
     * 
     * @param inputId the input identifier.
     * @param value the property value.
     */
    private void addParamValueToPropertyMap(String inputId, String value) {
        String key = step.getName() + "_" + inputId;
        List<String> values = propertyValues.get(key);
        if (values == null) {
            values = new ArrayList<String>();
            propertyValues.put(key, values);
        }
        values.add(value);
    }

    /**
     * Adds the parameter for one specified file.
     * 
     * @param params the list of parameters.
     * @param input the input being formatted.
     * @param path the full path to the selected file.
     */
    private void addParamForOneSpecifiedFile(JSONArray params, DataObject input, String path) {
        JSONObject param = new JSONObject();
        param.put("order", input.getOrderd());
        setParamNameAndValue(param, input.getSwitchString(), path);
        param.put("id", input.getId());
        param.put("multiplicity", input.getMultiplicityName());
        params.add(param);
    }

    /**
     * Updates all of the paths in the list of paths so that each path is relative to the parent of the user's home
     * directory. For now, we're assuming that all iRODS paths begin with the pattern, /&lt;zone&gt;/home/&lt;user&gt;.
     * 
     * @param paths the list of paths to update.
     * @return the updated paths.
     */
    private void updatePaths(List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            paths.set(i, paths.get(i).replaceAll("^/[^/]*/[^/]*", ""));
        }
    }
}
