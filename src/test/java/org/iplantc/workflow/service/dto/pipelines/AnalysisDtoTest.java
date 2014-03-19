package org.iplantc.workflow.service.dto.pipelines;

import org.iplantc.workflow.WorkflowException;
import java.util.List;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Template;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.junit.Before;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.pipelines.AnalysisDto.
 * 
 * @author Dennis Roberts
 */
public class AnalysisDtoTest {

    /**
     * Used to obtain mock data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * Initializes each unit test.
     */
    @Before
    public void initialize() {
        daoFactory = new MockDaoFactory();
        initializeDaoFactory();
    }

    /**
     * Verifies that we can construct an analysis DTO from the analysis identifier and name.
     */
    @Test
    public void testConstructionFromIdAndName() {
        AnalysisDto dto = new AnalysisDto("templateid", "analysisname");
        assertEquals("templateid", dto.getId());
        assertEquals("analysisname", dto.getName());
        assertEquals(0, dto.getInputs().size());
        assertEquals(0, dto.getOutputs().size());
    }

    /**
     * Verifies that we can construct an analysis DTO from an analysis.
     */
    @Test
    public void testConstructionFromAnalysis() {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById("unsteppedid");
        AnalysisDto dto = new AnalysisDto(analysis, daoFactory);
        assertEquals("step1templateid", dto.getId());
        assertEquals(analysis.getName(), dto.getName());


        List<PropertyDto> inputs = dto.getInputs();
        assertEquals(2, inputs.size());
        assertEquals("--step1templateinput1", inputs.get(0).getName());
        assertEquals("step1templateinput2prop", inputs.get(1).getName());

        List<PropertyDto> outputs = dto.getOutputs();
        assertEquals(2, outputs.size());
        assertEquals("--step1templateoutput1", outputs.get(0).getName());
        assertEquals("step1templateoutput2prop", outputs.get(1).getName());
    }

    /**
     * Verifies that we can construct an analysis DTO from a JSON object.
     */
    @Test
    public void testConstructionFromJson() {
        AnalysisDto dto = new AnalysisDto(createJson("bar"));
        assertEquals("barid", dto.getId());
        assertEquals("bar", dto.getName());
        assertEquals(0, dto.getInputs().size());
        assertEquals(0, dto.getOutputs().size());
    }

    /**
     * Verifies that we can construct an analysis DTO from a JSON string.
     */
    @Test
    public void testConstructionFromString() {
        AnalysisDto dto = new AnalysisDto(createJson("baz").toString());
        assertEquals("bazid", dto.getId());
        assertEquals("baz", dto.getName());
        assertEquals(0, dto.getInputs().size());
        assertEquals(0, dto.getOutputs().size());
    }

    /**
     * Verifies that we can generate JSON from an analysis DTO.
     */
    @Test
    public void testJsonGeneration() {
        JSONObject expected = createJson("quux");
        JSONObject actual = new AnalysisDto(expected).toJson();
        assertEquals(expected, actual);
    }

    /**
     * Verifies that we get an exception for an empty analysis.
     */
    @Test(expected = WorkflowException.class)
    public void shouldNotSupportEmptyAnalysis() {
        AnalysisDto dto = new AnalysisDto(UnitTestUtils.createAnalysis("empty"), daoFactory);
    }

    /**
     * Verifies that we get an exception for a multi-step analysis.
     */
    @Test(expected = WorkflowException.class)
    public void shouldNotSupportMultistepAnalysis() {
        AnalysisDto dto = new AnalysisDto(daoFactory.getTransformationActivityDao().findById("steppedid"), daoFactory);
    }

    /**
     * Verifies that the static factory method can successfully retrieve an analysis.
     */
    @Test
    public void testForAnalysisId() {
        AnalysisDto dto = AnalysisDto.forAnalysisId("unsteppedid", daoFactory);
        assertEquals("step1templateid", dto.getId());
        assertEquals("unstepped", dto.getName());

        List<PropertyDto> inputs = dto.getInputs();
        assertEquals(2, inputs.size());
        assertEquals("--step1templateinput1", inputs.get(0).getName());
        assertEquals("step1templateinput2prop", inputs.get(1).getName());

        List<PropertyDto> outputs = dto.getOutputs();
        assertEquals(2, outputs.size());
        assertEquals("--step1templateoutput1", outputs.get(0).getName());
        assertEquals("step1templateoutput2prop", outputs.get(1).getName());
    }

    /**
     * Verifies that the static factory method throws an exception if the analysis can't be found.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForBogusAnalysisId() {
        AnalysisDto.forAnalysisId("bogusid", daoFactory);
    }

    /**
     * Creates a JSON object for an analysis.
     * 
     * @param name the analysis name.
     * @return the JSON object.
     */
    private JSONObject createJson(String name) {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("id", name + "id");
        json.put("inputs", new JSONArray());
        json.put("outputs", new JSONArray());
        return json;
    }

    /**
     * Initializes the data access object factory.
     */
    private void initializeDaoFactory() {
        daoFactory.getTransformationActivityDao().save(createSingleStepAnalysis());
        daoFactory.getTransformationActivityDao().save(createMultistepAnalysis());
        daoFactory.getTemplateDao().save(createTemplate("step1template"));
        daoFactory.getTemplateDao().save(createTemplate("step2template"));
    }

    /**
     * Creates a template for testing.
     * 
     * @param name the name of the template.
     * @return the template.
     */
    private Template createTemplate(String name) {
        Template template = UnitTestUtils.createTemplate(name);
        template.addInputObject(createDataObject(name + "input1"));
        DataObject input2 = createDataObject(name + "input2");
        template.addInputObject(input2);
        template.addPropertyGroup(generatePropertyGroupForInput(input2));
        template.addOutputObject(createDataObject(name + "output1"));
        DataObject output2 = createDataObject(name + "output2");
        template.addOutputObject(output2);
        template.addPropertyGroup(generatePropertyGroupForOutput(output2));
        return template;
    }

    /**
     * Generates a property group for an input data object.
     * 
     * @param input the input data object.
     * @return the property group.
     */
    private PropertyGroup generatePropertyGroupForInput(DataObject input) {
        PropertyGroup group = generatePropertyGroupForDataObject(input);
        group.addProperty(generatePropertyForDataObject(input, "Input"));
        return group;
    }

    /**
     * Generates a property group for an output data object.
     * 
     * @param output the output data object.
     * @return the property group.
     */
    private PropertyGroup generatePropertyGroupForOutput(DataObject output) {
        PropertyGroup group = generatePropertyGroupForDataObject(output);
        group.addProperty(generatePropertyForDataObject(output, "Output"));
        return group;
    }

    /**
     * Generates a property for a data object.
     * 
     * @param dataObject the data object.
     * @param typeName the property type name.
     * @return the property.
     */
    private Property generatePropertyForDataObject(DataObject dataObject, String typeName) {
        Property prop = new Property();
        prop.setDataObject(dataObject);
        prop.setDefaultValue(dataObject.getName() + "prop");
        prop.setDescription(dataObject.getDescription() + "prop");
        prop.setId(dataObject.getId() + "prop");
        prop.setIsVisible(true);
        prop.setLabel(dataObject.getLabel() + "prop");
        prop.setName(dataObject.getName() + "prop");
        prop.setOmitIfBlank(true);
        prop.setOrder(dataObject.getOrderd());
        prop.setPropertyType(UnitTestUtils.createPropertyType(typeName));
        prop.setVisible(true);
        daoFactory.getPropertyDao().save(prop);
        return prop;
    }

    /**
     * Generates a property group for a data object.
     * 
     * @param dataObject the data object.
     * @return the property group.
     */
    private PropertyGroup generatePropertyGroupForDataObject(DataObject dataObject) {
        PropertyGroup group = new PropertyGroup();
        group.setDescription(dataObject.getDescription());
        group.setGroupType("step");
        group.setId(ImportUtils.generateId());
        group.setLabel(dataObject.getLabel());
        group.setName(dataObject.getName());
        group.setVisible(true);
        return group;
    }

    /**
     * Creates a data object for testing.
     * 
     * @param name the name of the data object.
     * @return the data object.
     */
    private DataObject createDataObject(String name) {
        DataObject dataObject = new DataObject();
        dataObject.setDataFormat(UnitTestUtils.createDataFormat("Unspecified"));
        dataObject.setDescription(name + "description");
        dataObject.setId(name + "id");
        dataObject.setInfoType(UnitTestUtils.createInfoType("Unknown"));
        dataObject.setLabel(name + "label");
        dataObject.setMultiplicity(UnitTestUtils.createMultiplicity("single"));
        dataObject.setName(name);
        dataObject.setOrderd(1);
        dataObject.setRequired(true);
        dataObject.setRetain(true);
        dataObject.setSwitchString("--" + name);
        return dataObject;
    }

    /**
     * Creates an analysis with a single step for testing.
     * 
     * @return the analysis.
     */
    private TransformationActivity createSingleStepAnalysis() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("unstepped");
        analysis.addStep(createStep("step1"));
        return analysis;
    }

    /**
     * Creates an analysis with steps for testing.
     * 
     * @return the analysis.
     */
    private TransformationActivity createMultistepAnalysis() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("stepped");
        analysis.addStep(createStep("step1"));
        analysis.addStep(createStep("step2"));
        analysis.addMapping(createInputOutputMapForAnalysis(analysis));
        return analysis;
    }

    /**
     * Creates a transformation step for testing.
     * 
     * @param name the name of the step.
     * @return the transformation step.
     */
    private TransformationStep createStep(String name) {
        TransformationStep step = new TransformationStep();
        step.setDescription(name + "description");
        step.setGuid(ImportUtils.generateId());
        step.setName(name);
        step.setTransformation(createTransformation(name));
        return step;
    }

    /**
     * Creates a transformation for testing.
     * 
     * @param stepName the name of the transformation step.
     * @return the transformation.
     */
    private Transformation createTransformation(String stepName) {
        Transformation transformation = new Transformation();
        transformation.setDescription(stepName + "transformationdescription");
        transformation.setName(stepName + "transformationname");
        transformation.setTemplate_id(stepName + "templateid");
        return transformation;
    }

    /**
     * Creates an input/output map for the stepped analysis.
     * 
     * @param analysis the analysis to generate the map for.
     * @return the map.
     */
    private InputOutputMap createInputOutputMapForAnalysis(TransformationActivity analysis) {
        InputOutputMap map = new InputOutputMap();
        map.setSource(analysis.step(0));
        map.setTarget(analysis.step(1));
        map.addAssociation("step1templateoutput1id", "step2templateinput1id");
        map.addAssociation("step1templateoutput2id", "step2templateinput2id");
        return map;
    }
}
