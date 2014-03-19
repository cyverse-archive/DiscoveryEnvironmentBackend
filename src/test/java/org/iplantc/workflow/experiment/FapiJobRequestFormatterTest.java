package org.iplantc.workflow.experiment;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.data.Multiplicity;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.PropertyType;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.user.UserDetails;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for org.iplantc.workflow.experiment.FapiJobRequestFormatter.
 *
 * @author Dennis Roberts
 */
public class FapiJobRequestFormatterTest {

    /**
     * The factory used to create mock data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The details of the user submitting the job.
     */
    private UserDetails userDetails;

    /**
     * The object used to ensure job name uniqueness.
     */
    private MockJobNameUniquenessEnsurer jobNameUniquenessEnsurer;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        initializeMockDaoFactory();
        createUserDetails();
        jobNameUniquenessEnsurer = new MockJobNameUniquenessEnsurer();
    }

    private void createUserDetails() {
        String username = "someuser@iplantcollaborative.org";
        String password = "S3cret";
        String email = "someuser@example.com";
        String shortUsername = "someuser";
        String firstName = "Some";
        String lastName = "User";
        userDetails = new UserDetails(username, password, email, shortUsername, firstName, lastName);
    }

    /**
     * Gets the short username (that is, the username without the domain name of the identity provider).
     *
     * @return the username.
     */
    private String getShortUsername() {
        return userDetails.getShortUsername();
    }

    /**
     * Initializes the mock DAO factory.
     */
    private void initializeMockDaoFactory() {
        daoFactory = new MockDaoFactory();
        daoFactory.getDeployedComponentDao().save(createDeployedComponent());
        daoFactory.getTemplateDao().save(createEmptyTemplate());
        daoFactory.getTemplateDao().save(createTemplateWithProperties());
        daoFactory.getTemplateDao().save(createTemplateWithInputs());
        daoFactory.getTemplateDao().save(createMultistepAnalysisTemplate1());
        daoFactory.getTemplateDao().save(createMultistepAnalysisTemplate2());
        daoFactory.getTransformationActivityDao().save(createEmptyAnalysis());
        daoFactory.getTransformationActivityDao().save(createAnalysisWithOneEmptyStep());
        daoFactory.getTransformationActivityDao().save(createAnalysisContainingOneTransformationWithProperties());
        daoFactory.getTransformationActivityDao().save(createAnalysisWithInputs());
        daoFactory.getTransformationActivityDao().save(createMultistepAnalysis());
    }

    /**
     * Creates the second template to use in our multistep analysis tests.
     *
     * @return the template.
     */
    private Template createMultistepAnalysisTemplate2() {
        Template template = new Template();
        template.setId("multistep_analysis_template_2");
        template.setComponent("deployed_component_id");
        template.addInputObject(createDataObject("sharedInput", 1, "--sharedIn=", "one"));
        template.addInputObject(createDataObject("chainedInput", 2, "--chainedIn=", "one"));
        template.addInputObject(createDataObject("templateInput", 3, "--templateIn=", "one"));
        return template;
    }

    /**
     * Creates the first template to use in our multistep analysis tests.
     *
     * @return the template.
     */
    private Template createMultistepAnalysisTemplate1() {
        Template template = new Template();
        template.setId("multistep_analysis_template_1");
        template.setComponent("deployed_component_id");
        template.addInputObject(createDataObject("sharedInput", 1, "--sharedIn=", "one"));
        template.addOutputObject(createDataObject("chainedOutput", "chained_output.txt", 2, "--chainedOut=", "one"));
        template.addOutputObject(createDataObject("templateOutput", 3, "--templateOut=", "one"));
        return template;
    }

    /**
     * Creates a template with some inputs.
     *
     * @return the template.
     */
    private Template createTemplateWithInputs() {
        Template template = new Template();
        template.setId("template_with_inputs");
        template.setComponent("deployed_component_id");
        template.addInputObject(createDataObject("inputFile", 1, "--in=", "one"));
        template.addInputObject(createDataObject("inputFolder", 2, "--folder=", "folder"));
        template.addInputObject(createDataObject("inputFiles", 3, "", "many"));
        template.addInputObject(createDataObject("templateInputFile", 4, "--tin=", "one"));
        return template;
    }

    /**
     * Creates a data object.
     *
     * @param id the data object identifier.
     * @param order the order specifier.
     * @param option the command-line option.
     * @param multiplicity the data object multiplicity setting.
     * @return the data object.
     */
    private DataObject createDataObject(String id, int order, String option, String multiplicity) {
        return createDataObject(id, id, order, option, multiplicity);
    }

    /**
     * Creates a data object.
     *
     * @param id the data object identifier.
     * @param name the data object name.
     * @param order the order specifier.
     * @param option the command-line option.
     * @param multiplicity the data object multiplicity setting.
     * @return the data object.
     */
    private DataObject createDataObject(String id, String name, int order, String option, String multiplicity) {
        DataObject dataObject = new DataObject();
        dataObject.setId(id);
        dataObject.setName(name);
        dataObject.setDescription(id);
        dataObject.setOrderd(order);
        dataObject.setSwitchString(option);
        dataObject.setMultiplicity(createMultiplicity(multiplicity));
        return dataObject;
    }

    /**
     * Creates a multiplicity object with the given name.
     *
     * @param name the multiplicity name.
     * @return the multiplicity object.
     */
    private Multiplicity createMultiplicity(String name) {
        Multiplicity multiplicity = new Multiplicity();
        multiplicity.setName(name);
        return multiplicity;
    }

    /**
     * Creates a template with some properties.
     *
     * @return the template.
     */
    private Template createTemplateWithProperties() {
        Template template = new Template();
        template.setId("template_with_properties");
        template.setComponent("deployed_component_id");
        template.addPropertyGroup(createFirstPropertyGroup());
        template.addPropertyGroup(createSecondPropertyGroup());
        return template;
    }

    /**
     * Creates the second property group to go in a template with properties.
     *
     * @return the property group.
     */
    private PropertyGroup createSecondPropertyGroup() {
        PropertyGroup propertyGroup = new PropertyGroup();
        propertyGroup.addProperty(createProperty("--third=", 3));
        propertyGroup.addProperty(createProperty("--fourth=", 4));
        propertyGroup.addProperty(createHiddenProperty("--fifth=", 5));
        propertyGroup.addProperty(createBooleanProperty("--sixth=", 6));
        return propertyGroup;
    }

    /**
     * Creates the first property group to go in a template with properties.
     *
     * @return the property group.
     */
    private PropertyGroup createFirstPropertyGroup() {
        PropertyGroup propertyGroup = new PropertyGroup();
        propertyGroup.addProperty(createProperty("--first=", 1));
        propertyGroup.addProperty(createProperty("--second=", 2));
        return propertyGroup;
    }

    /**
     * Creates a Boolean property with the given name.
     *
     * @param name the property name.
     * @param order the command-line order.
     * @return the property.
     */
    private Property createBooleanProperty(String name, int order) {
        Property property = createProperty(name, order);
        property.setPropertyType(new PropertyType("FlagId", "Flag", "", ""));
        property.setDefaultValue("false");
        return property;
    }

    /**
     * Creates a hidden property with the given name.
     *
     * @param name the property name.
     * @param order the command-line order.
     * @return the property.
     */
    private Property createHiddenProperty(String name, int order) {
        Property property = createProperty(name, order);
        property.setIsVisible(false);
        return property;
    }

    /**
     * Creates a property with the given name.
     *
     * @param name the property name.
     * @param order the command-line order.
     * @return the property.
     */
    private Property createProperty(String name, int order) {
        Property property = new Property();
        property.setId(name + " id");
        property.setName(name);
        property.setDescription(name + " description");
        property.setLabel(name + " label");
        property.setDefaultValue(name.replace("--", ""));
        property.setOrder(order);
        return property;
    }

    /**
     * Creates an empty template for testing.
     *
     * @return the template.
     */
    private Template createEmptyTemplate() {
        Template template = new Template();
        template.setId("empty_template");
        template.setComponent("deployed_component_id");
        return template;
    }

    /**
     * Creates a fake deployed component for testing.
     *
     * @return the deployed component.
     */
    private DeployedComponent createDeployedComponent() {
        DeployedComponent component = new DeployedComponent();
        component.setId("deployed_component_id");
        component.setName("deployed component name");
        component.setLocation("deployed component location");
        component.setDescription("deployed component description");
        component.setToolType(UnitTestUtils.createToolType("deployed component type"));
        return component;
    }

    /**
     * Creates the analysis to use in our multistep analysis tests.
     *
     * @return the analysis.
     */
    private TransformationActivity createMultistepAnalysis() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setDescription("description of multistep analysis");
        analysis.setId("multistep_analysis");
        analysis.setName("name of multistep analysis");
        TransformationStep step1 = createMultistepAnalysisStep1();
        TransformationStep step2 = createMultistepAnalysisStep2();
        analysis.addStep(step1);
        analysis.addStep(step2);
        analysis.addMapping(createMultistepInputOutputMap(step1, step2));
        return analysis;
    }

    /**
     * Creates the input/output mapping for our multistep analysis.
     *
     * @return the mapping.
     */
    private InputOutputMap createMultistepInputOutputMap(TransformationStep source, TransformationStep target) {
        InputOutputMap map = new InputOutputMap();
        map.setSource(source);
        map.setTarget(target);
        map.addAssociation("in#sharedInput", "sharedInput");
        map.addAssociation("chainedOutput", "chainedInput");
        map.addAssociation("templateOutput", "templateInput");
        return map;
    }

    /**
     * Creates the first transformation step in our multistep analysis.
     *
     * @return the transformation step.
     */
    private TransformationStep createMultistepAnalysisStep1() {
        TransformationStep step = new TransformationStep();
        step.setName("name of multistep analysis step 1");
        step.setGuid("multistep_analysis_step_1");
        step.setDescription("description of multistep analysis step 1");
        step.setTransformation(createMultistepAnalysisTransformation1());
        return step;
    }

    /**
     * Creates the first transformation in our multistep analysis.
     *
     * @return the transformation.
     */
    private Transformation createMultistepAnalysisTransformation1() {
        Transformation transformation = new Transformation();
        transformation.setName("name of multistep analysis transformation 1");
        transformation.setDescription("description of multistep analysis transformation 1");
        transformation.setTemplate_id("multistep_analysis_template_1");
        transformation.addPropertyValue("templateOutput", "template_template_output.txt");
        return transformation;
    }

    /**
     * Creates the second transformation step in our multistep analysis.
     *
     * @return the transformation step.
     */
    private TransformationStep createMultistepAnalysisStep2() {
        TransformationStep step = new TransformationStep();
        step.setName("name of multistep analysis step 2");
        step.setGuid("multistep_analysis_step_2");
        step.setDescription("description of multistep analysis step 2");
        step.setTransformation(createMultistepAnalysisTransformation2());
        return step;
    }

    /**
     * Creates the second transformation in our multistep analysis.
     *
     * @return the transformation.
     */
    private Transformation createMultistepAnalysisTransformation2() {
        Transformation transformation = new Transformation();
        transformation.setName("name of multistep analysis transformation 2");
        transformation.setDescription("description of multistep analysis transformation 2");
        transformation.setTemplate_id("multistep_analysis_template_2");
        return transformation;
    }

    /**
     * Creates an analysis with some inputs.
     *
     * @return the analysis.
     */
    private TransformationActivity createAnalysisWithInputs() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setDescription("description of analysis wiht inputs");
        analysis.setId("analysis_with_inputs");
        analysis.setName("name of analysis with properties");
        analysis.addStep(createTransformationStepWithInputs());
        return analysis;
    }

    /**
     * Creates a transformation step with some inputs.
     *
     * @return the transformation step.
     */
    private TransformationStep createTransformationStepWithInputs() {
        TransformationStep step = new TransformationStep();
        step.setName("name of step with inputs");
        step.setGuid("step_with_inputs");
        step.setDescription("description of step with inputs");
        step.setTransformation(createTransformationWithInputs());
        return step;
    }

    /**
     * Creates a transformation with some inputs.
     *
     * @return the transformation.
     */
    private Transformation createTransformationWithInputs() {
        Transformation transformation = new Transformation();
        transformation.setName("transformation with inputs");
        transformation.setDescription("description of transformation with inputs");
        transformation.setTemplate_id("template_with_inputs");
        transformation.addPropertyValue("templateInputFile", "/iPlant/home/someuser/baz");
        return transformation;
    }

    /**
     * Creates an analysis containing one transformation with properties.
     *
     * @return the analysis.
     */
    private TransformationActivity createAnalysisContainingOneTransformationWithProperties() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setDescription("description of analysis with properties");
        analysis.setId("analysis_with_properties");
        analysis.setName("name of analysis with properties");
        analysis.addStep(createTransformationStepWithProperties());
        return analysis;
    }

    /**
     * Creates a transformation step containing one transformation with properties.
     *
     * @return the transformation.
     */
    private TransformationStep createTransformationStepWithProperties() {
        TransformationStep step = new TransformationStep();
        step.setName("name of step with properties");
        step.setGuid("step_with_properties");
        step.setDescription("description of step with properties");
        step.setTransformation(createTransformationWithProperties());
        return step;
    }

    /**
     * Creates a transformation with properties.
     *
     * @return the transformation.
     */
    private Transformation createTransformationWithProperties() {
        Transformation transformation = new Transformation();
        transformation.setName("transformation with properties");
        transformation.setDescription("description of transformation with properties");
        transformation.setTemplate_id("template_with_properties");
        return transformation;
    }

    /**
     * Creates an analysis with one empty transformation step.
     *
     * @return the analysis.
     */
    private TransformationActivity createAnalysisWithOneEmptyStep() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setDescription("description of analysis with one empty step");
        analysis.setId("analysis_with_one_empty_step");
        analysis.setName("name of analysis with one empty step");
        analysis.addStep(createEmptyTransformationStep());
        return analysis;
    }

    /**
     * Creates an empty transformation step.
     *
     * @return the transformation step.
     */
    private TransformationStep createEmptyTransformationStep() {
        TransformationStep step = new TransformationStep();
        step.setName("empty step name");
        step.setGuid("empty_step");
        step.setDescription("empty step description");
        step.setTransformation(createEmptyTransformation());
        return step;
    }

    /**
     * Creates an empty transformation.
     *
     * @return the transformation.
     */
    private Transformation createEmptyTransformation() {
        Transformation transformation = new Transformation();
        transformation.setName("empty transformation");
        transformation.setDescription("empty transformation description");
        transformation.setTemplate_id("empty_template");
        return transformation;
    }

    /**
     * Creates an empty analysis for testing.
     *
     * @return the empty analysis.
     */
    private TransformationActivity createEmptyAnalysis() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setDescription("empty analysis description");
        analysis.setId("empty_analysis");
        analysis.setName("empty analysis name");
        return analysis;
    }

    /**
     * Verifies that we can format a job submission request for an empty job.
     */
    @Test
    public void testEmptyJob() {
        JSONObject experiment = createEmptyExperiment("empty_analysis");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        assertEquals("empty analysis description", submission.getString("analysis_description"));
        assertEquals("empty_analysis", submission.getString("analysis_id"));
        assertEquals("empty analysis name", submission.getString("analysis_name"));
        assertEquals("empty description", submission.getString("description"));
        assertEquals("condor", submission.getString("execution_target"));
        assertEquals("empty", submission.getString("name"));
        assertEquals("false", submission.getString("notify"));
        assertEquals("submit", submission.getString("request_type"));
        assertEquals("someuser", submission.getString("username"));
        assertTrue(submission.getString("uuid").matches("[-0-9A-F]{36}"));
        assertEquals("1", submission.getString("workspace_id"));
    }

    /**
     * Creates an empty experiment.
     *
     * @return the experiment.
     */
    private JSONObject createEmptyExperiment(String analysisId) {
        JSONObject json = new JSONObject();
        json.put("name", "empty");
        json.put("description", "empty description");
        json.put("notify", false);
        json.put("workspace_id", 1);
        json.put("analysis_id", analysisId);
        json.put("outputDirectory", "/iplant/home/someuser/analyses");
        return json;
    }

    /**
     * Verifies that we can format a job submission request for a job with one empty step.
     */
    @Test
    public void testJobWithOneEmptyStep() {
        JSONObject experiment = createEmptyExperiment("analysis_with_one_empty_step");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        JSONArray steps = submission.getJSONArray("steps");
        assertEquals(1, steps.size());

        JSONObject step = steps.getJSONObject(0);
        assertEquals("empty step name", step.getString("name"));
        assertEquals("condor", step.getString("type"));
        assertNotNull(step.getJSONObject("component"));
        assertTrue(step.has("config"));

        JSONObject component = step.getJSONObject("component");
        assertEquals("deployed component name", component.getString("name"));
        assertEquals("deployed component location", component.getString("location"));
        assertEquals("deployed component type", component.getString("type"));
        assertEquals("deployed component description", component.getString("description"));
    }

    /**
     * Verifies that we can format a job submission request for a job with properties.
     */
    @Test
    public void testJobContainingOneStepWithProperties() {
        JSONObject experiment = createEmptyExperiment("analysis_with_properties");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        JSONArray steps = submission.getJSONArray("steps");
        assertEquals(1, steps.size());

        JSONObject step = steps.getJSONObject(0);
        JSONObject config = step.getJSONObject("config");
        assertNotNull(config);

        JSONArray params = config.getJSONArray("params");
        assertNotNull(params);
        assertEquals(10, params.size());

        validateParam(0, "", "run", "command", params.getJSONObject(0));
        validateParam(1, "", "--proxy_user=" + getShortUsername(), "proxyUser", params.getJSONObject(1));
        validateParam(1, "", "--jobName=empty", "jobName", params.getJSONObject(2));
        validateParam(1, "", "--archive", "archiveResults", params.getJSONObject(3));
        validateParam(1, "", "--archivePath=/someuser/analyses/empty", "archivePath", params.getJSONObject(4));
        validateParam(1, "", "--first=first=", "--first= id", params.getJSONObject(5));
        validateParam(2, "", "--second=second=", "--second= id", params.getJSONObject(6));
        validateParam(3, "", "--third=third=", "--third= id", params.getJSONObject(7));
        validateParam(4, "", "--fourth=fourth=", "--fourth= id", params.getJSONObject(8));
        validateParam(5, "", "--fifth=fifth=", "--fifth= id", params.getJSONObject(9));
    }

    /**
     * Verifies that we can format a submission request for a job with specified property values.
     */
    @Test
    public void testJobWithSpecifiedPropertyValues() {
        JSONObject experiment = createExperimentWithConfig("analysis_with_properties");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        JSONArray steps = submission.getJSONArray("steps");
        assertEquals(1, steps.size());

        JSONObject step = steps.getJSONObject(0);
        JSONObject config = step.getJSONObject("config");
        assertNotNull(config);

        JSONArray inputs = config.getJSONArray("input");
        assertNotNull(inputs);
        assertEquals(0, inputs.size());

        JSONArray outputs = config.getJSONArray("output");
        assertNotNull(outputs);
        assertEquals(1, outputs.size());

        JSONObject output1 = outputs.getJSONObject(0);
        assertEquals("logs", output1.getString("name"));
        assertEquals("logs", output1.getString("property"));
        assertEquals("File", output1.getString("type"));
        assertEquals("collection", output1.getString("multiplicity"));
        assertTrue(output1.getBoolean("retain"));

        JSONArray params = config.getJSONArray("params");
        assertNotNull(params);
        assertEquals(11, params.size());

        validateParam(0, "", "run", "command", params.getJSONObject(0));
        validateParam(1, "", "--proxy_user=" + getShortUsername(), "proxyUser", params.getJSONObject(1));
        validateParam(1, "", "--jobName=config", "jobName", params.getJSONObject(2));
        validateParam(1, "", "--archive", "archiveResults", params.getJSONObject(3));
        validateParam(1, "", "--archivePath=/someuser/analyses/config", "archivePath", params.getJSONObject(4));
        validateParam(1, "", "--first=one", "--first= id", params.getJSONObject(5));
        validateParam(2, "", "--second=two", "--second= id", params.getJSONObject(6));
        validateParam(3, "", "--third=three", "--third= id", params.getJSONObject(7));
        validateParam(4, "", "--fourth=four", "--fourth= id", params.getJSONObject(8));
        validateParam(5, "", "--fifth=fifth=", "--fifth= id", params.getJSONObject(9));
        validateParam(6, "", "--sixth=", "--sixth= id", params.getJSONObject(10));
    }

    /**
     * Verifies that we can format a job for an analysis with inputs.
     */
    @Test
    public void testJobWithInputs() {
        JSONObject experiment = createExperimentWithConfig("analysis_with_inputs");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        JSONArray steps = submission.getJSONArray("steps");
        assertEquals(1, steps.size());

        JSONObject step = steps.getJSONObject(0);
        JSONObject config = step.getJSONObject("config");
        assertNotNull(config);

        JSONArray params = config.getJSONArray("params");
        assertNotNull(params);
        assertEquals(10, params.size());

        validateParam(0, "", "run", "command", params.getJSONObject(0));
        validateParam(1, "", "--proxy_user=" + getShortUsername(), "proxyUser", params.getJSONObject(1));
        validateParam(1, "", "--jobName=config", "jobName", params.getJSONObject(2));
        validateParam(1, "", "--archive", "archiveResults", params.getJSONObject(3));
        validateParam(1, "", "--archivePath=/someuser/analyses/config", "archivePath", params.getJSONObject(4));
        validateParam(1, "", "--in=/someuser/somefile.txt", "inputFile", "one", params.getJSONObject(5));
        validateParam(2, "", "--folder=/someuser/somefolder", "inputFolder", "folder", params.getJSONObject(6));
        validateParam(3, "", "/someuser/foo", "inputFiles", "many", params.getJSONObject(7));
        validateParam(3, "", "/someuser/bar", "inputFiles", "many", params.getJSONObject(8));
        validateParam(4, "", "--tin=/someuser/baz", "templateInputFile", "one", params.getJSONObject(9));
    }

    /**
     * Verifies that we can format a job for a multistep analysis.
     */
    @Test
    public void testMultistepAnalysis() {
        JSONObject experiment = createExperimentWithConfig("multistep_analysis");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        JSONArray steps = submission.getJSONArray("steps");
        assertEquals(2, steps.size());

        JSONObject step1 = steps.getJSONObject(0);
        JSONObject config1 = step1.getJSONObject("config");
        assertNotNull(config1);

        JSONArray step1Params = config1.getJSONArray("params");
        assertNotNull(step1Params);
        assertEquals(8, step1Params.size());

        validateParam(0, "", "run", "command", step1Params.getJSONObject(0));
        validateParam(1, "", "--proxy_user=" + getShortUsername(), "proxyUser", step1Params.getJSONObject(1));
        validateParam(1, "", "--jobName=config", "jobName", step1Params.getJSONObject(2));
        validateParam(1, "", "--archive", "archiveResults", step1Params.getJSONObject(3));
        validateParam(1, "", "--archivePath=/someuser/analyses/config", "archivePath", step1Params.getJSONObject(4));
        validateParam(1, "", "--sharedIn=/someuser/shared_input.txt", "sharedInput", "one",
                step1Params.getJSONObject(5));
        validateParam(2, "", "--chainedOut=chained_output.txt", "chainedOutput", "one", step1Params.getJSONObject(6));
        validateParam(3, "", "--templateOut=template_template_output.txt", "templateOutput", "one",
                step1Params.getJSONObject(7));

        JSONObject step2 = steps.getJSONObject(1);
        JSONObject config2 = step2.getJSONObject("config");
        assertNotNull(config2);

        JSONArray step2Params = config2.getJSONArray("params");
        assertNotNull(step2Params);
        assertEquals(8, step2Params.size());

        validateParam(0, "", "run", "command", step2Params.getJSONObject(0));
        validateParam(1, "", "--proxy_user=" + getShortUsername(), "proxyUser", step2Params.getJSONObject(1));
        validateParam(1, "", "--jobName=config", "jobName", step2Params.getJSONObject(2));
        validateParam(1, "", "--archive", "archiveResults", step2Params.getJSONObject(3));
        validateParam(1, "", "--archivePath=/someuser/analyses/config", "archivePath", step2Params.getJSONObject(4));
        validateParam(1, "", "--sharedIn=/someuser/shared_input.txt", "sharedInput", "one",
                step2Params.getJSONObject(5));
        validateParam(2, "", "--chainedIn=chained_output.txt", "chainedInput", "one", step2Params.getJSONObject(6));
        validateParam(3, "", "--templateIn=template_template_output.txt", "templateInput", "one",
                step2Params.getJSONObject(7));
    }

    /**
     * Verifies that the experiment name is updated on the fly if it's not unique.
     */
    @Test
    public void testNonUniqueJobName() {
        jobNameUniquenessEnsurer.addJobName("empty");
        JSONObject experiment = createEmptyExperiment("analysis_with_one_empty_step");
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        assertEquals("empty-1", submission.getString("name"));
        assertEquals("empty", submission.get("display_name"));
    }

    /**
     * Verifies that an experiment with debugging enabled is formatted correctly.
     */
    @Test
    public void testExperimentWithDebuggingEnabled() {
        JSONObject experiment = createExperimentWithConfig("analysis_with_properties", true);
        JSONObject submission = createFormatter(experiment).formatJobRequest();
        JSONArray steps = submission.getJSONArray("steps");
        assertEquals(1, steps.size());

        JSONObject step = steps.getJSONObject(0);
        JSONObject config = step.getJSONObject("config");
        assertNotNull(config);

        JSONArray inputs = config.getJSONArray("input");
        assertNotNull(inputs);
        assertEquals(0, inputs.size());

        JSONArray outputs = config.getJSONArray("output");
        assertNotNull(outputs);
        assertEquals(1, outputs.size());

        JSONObject output1 = outputs.getJSONObject(0);
        assertEquals("logs", output1.getString("name"));
        assertEquals("logs", output1.getString("property"));
        assertEquals("File", output1.getString("type"));
        assertEquals("collection", output1.getString("multiplicity"));
        assertTrue(output1.getBoolean("retain"));
    }

    /**
     * Creates an experiment with a configuration.
     *
     * @param analysisId the analysis identifier.
     * @return the experiment.
     */
    private JSONObject createExperimentWithConfig(String analysisId) {
        return createExperimentWithConfig(analysisId, false);
    }

    /**
     * Creates an experiment with a configuration.
     *
     * @param analysisId the analysis identifier.
     * @param debug true if debugging should be enabled.
     * @return the experiment.
     */
    private JSONObject createExperimentWithConfig(String analysisId, boolean debug) {
        JSONObject json = new JSONObject();
        json.put("name", "config");
        json.put("description", "config");
        json.put("notify", false);
        json.put("workspace_id", 1);
        json.put("analysis_id", analysisId);
        json.put("debug", debug);
        json.put("outputDirectory", "/iplant/home/someuser/analyses");
        json.put("config", createExperimentConfiguration());
        return json;
    }

    /**
     * Creates a configuration for the experiment with a configuration.
     *
     * @return the configuration.
     */
    private JSONObject createExperimentConfiguration() {
        JSONObject json = new JSONObject();
        json.put("name of step with properties_--first= id", "one");
        json.put("name of step with properties_--second= id", "two");
        json.put("name of step with properties_--third= id", "three");
        json.put("name of step with properties_--fourth= id", "four");
        json.put("name of step with properties_--sixth= id", "true");
        json.put("name of step with inputs_inputFile", "/iPlant/home/someuser/somefile.txt");
        json.put("name of step with inputs_inputFolder", "/iPlant/home/someuser/somefolder");
        json.put("name of step with inputs_inputFiles",
                "[\"/iPlant/home/someuser/foo\", \"/iPlant/home/someuser/bar\"]");
        json.put("name of multistep analysis step 1_sharedInput", "/iPlant/home/someuser/shared_input.txt");
        return json;
    }

    /**
     * Creates a job request formatter for the given experiment.
     *
     * @param experiment the experiment.
     * @return the new job request formatter.
     */
    protected FapiJobRequestFormatter createFormatter(JSONObject experiment) {
        String irodsHome = "/iplant/home";
        return new FapiJobRequestFormatter(daoFactory, userDetails, experiment, jobNameUniquenessEnsurer, irodsHome);
    }

    /**
     * Validates a single parameter.
     *
     * @param order the order specifier.
     * @param name the parameter name.
     * @param value the parameter value.
     * @param id the parameter ID.
     * @param param the parameter.
     */
    private void validateParam(int order, String name, String value, String id, JSONObject param) {
        assertEquals(order, param.getInt("order"));
        assertEquals(name, param.getString("name"));
        assertEquals(value, param.getString("value"));
        assertEquals(id, param.getString("id"));
    }

    /**
     * Validates a single parameter with a multiplicity.
     *
     * @param order the order specifier.
     * @param name the parameter name.
     * @param value the parameter value.
     * @param id the parameter ID.
     * @param multiplicity the multiplicity name.
     * @param param the parameter.
     */
    private void validateParam(int order, String name, String value, String id, String multiplicity, JSONObject param) {
        validateParam(order, name, value, id, param);
        assertEquals(multiplicity, param.getString("multiplicity"));
    }
}
