package org.iplantc.workflow.service.dto.pipelines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONObject;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.PropertyDao;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;
import org.iplantc.workflow.service.util.PipelineAnalysisValidator;
import org.iplantc.workflow.service.util.UnreferencedInputFinder;
import org.iplantc.workflow.service.util.UnreferencedOutputFinder;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * A data transfer object used to describe analyses in a pipeline.
 * 
 * @author Dennis Roberts
 */
public class AnalysisDto extends AbstractDto {

    /**
     * The template identifier.
     */
    @JsonField(name = "id")
    private String id;

    /**
     * The analysis name.
     */
    @JsonField(name = "name")
    private String name;

    /**
     * The list of inputs to the analysis.
     */
    @JsonField(name = "inputs")
    private List<PropertyDto> inputs;

    /**
     * The list of outputs from the analysis.
     */
    @JsonField(name = "outputs")
    private List<PropertyDto> outputs;

    /**
     * @return the template ID.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the analysis name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the list of inputs.
     */
    public List<PropertyDto> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    /**
     * @return the list of outputs.
     */
    public List<PropertyDto> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    /**
     * @param id the analysis identifier.
     * @param name the analysis name.
     */
    public AnalysisDto(String id, String name) {
        this.id = id;
        this.name = name;
        this.inputs = new ArrayList<PropertyDto>();
        this.outputs = new ArrayList<PropertyDto>();
    }

    /**
     * @param analysis the analysis represented by this DTO.
     * @param daoFactory used to obtain data access objects.
     */
    public AnalysisDto(TransformationActivity analysis, DaoFactory daoFactory) {
        PipelineAnalysisValidator.validateAnalysis(analysis);
        id = analysis.getSteps().get(0).getTemplateId();
        name = analysis.getName();
        inputs = generateInputDtos(new UnreferencedInputFinder(analysis, daoFactory).findDataObjects(), daoFactory);
        outputs = generateOutputDtos(new UnreferencedOutputFinder(analysis, daoFactory).findDataObjects(), daoFactory);
    }

    /**
     * A static factory method that looks up the source analysis using an analysis identifier.
     * 
     * @param analysisId the analysis identifier.
     * @param daoFactory used to obtain data access objects.
     */
    public static AnalysisDto forAnalysisId(String analysisId, DaoFactory daoFactory) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis, " + analysisId + ", not found");
        }
        return new AnalysisDto(analysis, daoFactory);
    }

    /**
     * @param json a JSON object representing the analysis.
     */
    public AnalysisDto(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str a JSON string representing the analysis.
     */
    public AnalysisDto(String str) {
        fromString(str);
    }

    /**
     * Generates a list of property data transfer objects for a list of inputs.
     * 
     * @param inputs the list of inputs.
     * @param daoFactory used to obtain data access objects.
     * @return the list of property data transfer objects.
     */
    private List<PropertyDto> generateInputDtos(List<DataObject> inputs, DaoFactory daoFactory) {
        final PropertyDao propertyDao = daoFactory.getPropertyDao();
        return ListUtils.map(new Lambda<DataObject, PropertyDto>() {
            @Override
            public PropertyDto call(DataObject arg) {
                Property property = propertyDao.getPropertyForDataObject(arg);
                return property == null ? PropertyDto.fromInput(arg) : new PropertyDto(property);
            }
        }, inputs);
    }

    /**
     * Generates a list of property data transfer objects for a list of outputs.
     * 
     * @param outputs the list of outputs.
     * @param daoFactory used to obtain data access objects.
     * @return the list of property data transfer objects.
     */
    private List<PropertyDto> generateOutputDtos(List<DataObject> outputs, DaoFactory daoFactory) {
        final PropertyDao propertyDao = daoFactory.getPropertyDao();
        return ListUtils.map(new Lambda<DataObject, PropertyDto>() {
            @Override
            public PropertyDto call(DataObject arg) {
                Property property = propertyDao.getPropertyForDataObject(arg);
                return property == null ? PropertyDto.fromOutput(arg) : new PropertyDto(property);
            }
        }, outputs);
    }
}
