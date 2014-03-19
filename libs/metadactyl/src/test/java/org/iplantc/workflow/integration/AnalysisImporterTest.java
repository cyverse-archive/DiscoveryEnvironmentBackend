package org.iplantc.workflow.integration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.step.TransformationStep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONObject;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONArray;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.dao.mock.MockTemplateDao;
import org.iplantc.workflow.dao.mock.MockTransformationActivityDao;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.mock.MockWorkspaceInitializer;
import org.iplantc.workflow.service.UserService;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.AnalysisImporter.
 * 
 * @author Dennis Roberts
 */
public class AnalysisImporterTest {

    /**
     * The name of the user's root analysis group.
     */
    private static final String ROOT_ANALYSIS_GROUP = "Workspace";

    /**
     * The name of the development analysis group.
     */
    private static final String DEV_ANALYSIS_GROUP = "Dev";

    /**
     * The name of the favorites analysis group.
     */
    private static final String FAVES_ANALYSIS_GROUP = "Faves";

    /**
     * The list of analysis subgroup names.
     */
    private static final List<String> ANALYSIS_SUBGROUPS = Arrays.asList(DEV_ANALYSIS_GROUP, FAVES_ANALYSIS_GROUP);

    /**
     * Used to generate mock data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * Used to import template groups.
     */
    private TemplateGroupImporter templateGroupImporter;

    /**
     * The AnalysisImporter instance being tested.
     */
    private AnalysisImporter importer;

    /**
     * The user service used to initialize the user's workspace.
     */
    private UserService userService;

    /**
     * The initializer that calls the user service.
     */
    private MockWorkspaceInitializer workspaceInitializer;

    /**
     * Initializes each unit test
     */
    @Before
    public void initialize() {
        daoFactory = new MockDaoFactory();
        initializeTemplateGroupDao();
        initializeTemplateDao();
        initializeTemplateGroupImporter();
        initializeUserService();
        initializeWorkspaceInitializer();
        importer = new AnalysisImporter(daoFactory, templateGroupImporter, workspaceInitializer);
    }

    /**
     * Initializes the template group importer.
     */
    private void initializeTemplateGroupImporter() {
        int devIndex = ANALYSIS_SUBGROUPS.indexOf(DEV_ANALYSIS_GROUP);
        int favesIndex = ANALYSIS_SUBGROUPS.indexOf(FAVES_ANALYSIS_GROUP);
        templateGroupImporter = new TemplateGroupImporter(daoFactory, devIndex, favesIndex);
    }

    /**
     * Initializes the object used to initialize the the user's workspace.
     */
    private void initializeWorkspaceInitializer() {
        workspaceInitializer = new MockWorkspaceInitializer(userService);
    }

    /**
     * Initializes the service that actually initializes the user's workspace.
     */
    private void initializeUserService() {
        userService = new UserService();
        userService.setRootAnalysisGroup(ROOT_ANALYSIS_GROUP);
        userService.setDefaultAnalysisGroups(new JSONArray(ANALYSIS_SUBGROUPS).toString());
    }

    /**
     * Initializes the mock template group DAO to use for all of the tests.
     */
    private void initializeTemplateGroupDao() {
        UnitTestUtils.addRootTemplateGroup(daoFactory.getMockTemplateGroupDao());
    }

    /**
     * Initializes the template data access object to use for testing.
     */
    private void initializeTemplateDao() {
        MockTemplateDao templateDao = daoFactory.getMockTemplateDao();
        templateDao.save(createTemplate("templateid"));
        templateDao.save(createTemplate("othertemplateid"));
    }

    /**
     * Creates a template.
     * 
     * @param id the template identifier.
     * @return the template.
     */
    private Template createTemplate(String id) {
        Template template = new Template();
        template.setId(id);
        return template;
    }

    /**
     * @return the mock analysis data access object.
     */
    private MockTransformationActivityDao getAnalysisDao() {
        return daoFactory.getMockTransformationActivityDao();
    }

    /**
     * Verifies that we can import a fully specified analysis.
     * 
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testFullySpecifiedAnalysis() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_analysis");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(0);
        assertEquals("analysisid", analysis.getId());
        assertEquals("analysisname", analysis.getName());
        assertEquals("analysisdescription", analysis.getDescription());
        assertEquals("analysistype", analysis.getType());
        assertEquals(2, analysis.getSteps().size());
        assertEquals(1, analysis.getMappings().size());

        TransformationStep step1 = analysis.getSteps().get(0);
        assertEquals("stepid", step1.getGuid());
        assertEquals("stepname", step1.getName());
        assertEquals("stepdescription", step1.getDescription());

        Transformation transformation1 = step1.getTransformation();
        assertEquals("templateid", transformation1.getTemplate_id());
        assertEquals(2, transformation1.getPropertyValues().size());
        assertEquals("propertyvalue1", transformation1.getPropertyValues().get("propertyid1"));
        assertEquals("propertyvalue2", transformation1.getPropertyValues().get("propertyid2"));

        TransformationStep step2 = analysis.getSteps().get(1);
        assertEquals("otherstepid", step2.getGuid());
        assertEquals("otherstepname", step2.getName());
        assertEquals("otherstepdescription", step2.getDescription());

        Transformation transformation2 = step2.getTransformation();
        assertEquals("othertemplateid", transformation2.getTemplate_id());
        assertEquals(2, transformation2.getPropertyValues().size());
        assertEquals("othervalue1", transformation2.getPropertyValues().get("otherid1"));
        assertEquals("othervalue2", transformation2.getPropertyValues().get("otherid2"));

        InputOutputMap mapping = analysis.getMappings().get(0);
        assertSame(step1, mapping.getSource());
        assertSame(step2, mapping.getTarget());
    }

    /**
     * Verifies that we can import a minimally specified analysis.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test
    public void testMinimallySpecifiedAnalysis() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("minimally_specified_analysis");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(0);
        assertTrue(analysis.getId().matches("[-0-9A-F]{36}"));
        assertEquals("analysisname", analysis.getName());
        assertEquals("analysisdescription", analysis.getDescription());
        assertEquals("", analysis.getType());
        assertEquals(2, analysis.getSteps().size());
        assertEquals(0, analysis.getMappings().size());

        TransformationStep step1 = analysis.getSteps().get(0);
        assertEquals("stepid", step1.getGuid());
        assertEquals("stepname", step1.getName());
        assertEquals("stepdescription", step1.getDescription());

        Transformation transformation1 = step1.getTransformation();
        assertEquals("templateid", transformation1.getTemplate_id());
        assertEquals(2, transformation1.getPropertyValues().size());
        assertEquals("propertyvalue1", transformation1.getPropertyValues().get("propertyid1"));
        assertEquals("propertyvalue2", transformation1.getPropertyValues().get("propertyid2"));

        TransformationStep step2 = analysis.getSteps().get(1);
        assertEquals("otherstepid", step2.getGuid());
        assertEquals("otherstepname", step2.getName());
        assertEquals("otherstepdescription", step2.getDescription());

        Transformation transformation2 = step2.getTransformation();
        assertEquals("othertemplateid", transformation2.getTemplate_id());
        assertEquals(2, transformation2.getPropertyValues().size());
        assertEquals("othervalue1", transformation2.getPropertyValues().get("otherid1"));
        assertEquals("othervalue2", transformation2.getPropertyValues().get("otherid2"));
    }

    /**
     * Verifies that we can explicitly request automatic ID generation.
     * 
     * @throws JSONException if the JSON object is invalid.
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testAutoGeneratedId() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("analysis_with_explicit_id_generation");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());
        
        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(0);
        assertTrue(analysis.getId().matches("[-0-9A-F]{36}"));
        assertEquals("analysisname", analysis.getName());
        assertEquals("analysisdescription", analysis.getDescription());
        assertEquals("", analysis.getType());
        assertEquals(2, analysis.getSteps().size());
        assertEquals(0, analysis.getMappings().size());

        TransformationStep step1 = analysis.getSteps().get(0);
        assertEquals("stepid", step1.getGuid());
        assertEquals("stepname", step1.getName());
        assertEquals("stepdescription", step1.getDescription());

        Transformation transformation1 = step1.getTransformation();
        assertEquals("templateid", transformation1.getTemplate_id());
        assertEquals(2, transformation1.getPropertyValues().size());
        assertEquals("propertyvalue1", transformation1.getPropertyValues().get("propertyid1"));
        assertEquals("propertyvalue2", transformation1.getPropertyValues().get("propertyid2"));

        TransformationStep step2 = analysis.getSteps().get(1);
        assertEquals("otherstepid", step2.getGuid());
        assertEquals("otherstepname", step2.getName());
        assertEquals("otherstepdescription", step2.getDescription());

        Transformation transformation2 = step2.getTransformation();
        assertEquals("othertemplateid", transformation2.getTemplate_id());
        assertEquals(2, transformation2.getPropertyValues().size());
        assertEquals("othervalue1", transformation2.getPropertyValues().get("otherid1"));
        assertEquals("othervalue2", transformation2.getPropertyValues().get("otherid2"));
    }

    /**
     * Verifies that a missing analysis name causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingAnalysisNameShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_id\": \"templateid\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\",\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing analysis description causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingAnalysisDescriptionShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_id\": \"templateid\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\",\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing list of steps causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingStepListShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that the importer can handle a step with an unspecified identifier.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test
    public void testMissingStepId() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_id\": \"templateid\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\"\n"
                + "            }\n"
                + "        }\n"
                + "    ],\n"
                + "    \"implementation\": {\n"
                + "        \"implementor\": \"bob\",\n"
                + "        \"implementor_email\": \"bob@bob-like.net\",\n"
                + "        \"links\": [ \"www.google.com\", \"www.iplantcollaborative.org\"],\n"
                + "        \"test\": {\n"
                + "            \"params\": [\"-a\", \"foo\"],\n"
                + "            \"input_files\": [\"foo.txt\"],\n"
                + "            \"output_files\": [\"rahr.data\"]\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());
    }

    /**
     * Verifies that a missing step name causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingStepNameShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_id\": \"templateid\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\",\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing step description causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingStepDescriptionShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"template_id\": \"templateid\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\",\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing template ID causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingTemplateIdShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\",\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing step configuration causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingStepConfigurationShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_id\": \"templateid\",\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that an unknown template ID causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = WorkflowException.class)
    public void unknownTemplateIdShouldGenerateException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_id\": \"unknowntemplateid\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\"\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that we can import multiple analyses.
     * 
     * @throws JSONException if the JSON array doesn't meet the requirements of the importer.
     */
    @Test
    public void testMultipleAnalyses() throws JSONException, IOException {
        JSONArray array = getTestJSONArray("multiple_analyses");

        importer.importObjectList(array);
        assertEquals(2, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis1 = getAnalysisDao().getSavedObjects().get(0);
        assertEquals("analysisid", analysis1.getId());
        assertEquals("analysisname", analysis1.getName());
        assertEquals("analysisdescription", analysis1.getDescription());
        assertEquals("analysistype", analysis1.getType());
        assertEquals(2, analysis1.getSteps().size());
        assertEquals(1, analysis1.getMappings().size());

        TransformationStep step1 = analysis1.getSteps().get(0);
        assertEquals("stepid", step1.getGuid());
        assertEquals("stepname", step1.getName());
        assertEquals("stepdescription", step1.getDescription());

        Transformation transformation1 = step1.getTransformation();
        assertEquals("templateid", transformation1.getTemplate_id());
        assertEquals(2, transformation1.getPropertyValues().size());
        assertEquals("propertyvalue1", transformation1.getPropertyValues().get("propertyid1"));
        assertEquals("propertyvalue2", transformation1.getPropertyValues().get("propertyid2"));

        TransformationStep step2 = analysis1.getSteps().get(1);
        assertEquals("otherstepid", step2.getGuid());
        assertEquals("otherstepname", step2.getName());
        assertEquals("otherstepdescription", step2.getDescription());

        Transformation transformation2 = step2.getTransformation();
        assertEquals("othertemplateid", transformation2.getTemplate_id());
        assertEquals(2, transformation2.getPropertyValues().size());
        assertEquals("othervalue1", transformation2.getPropertyValues().get("otherid1"));
        assertEquals("othervalue2", transformation2.getPropertyValues().get("otherid2"));

        InputOutputMap mapping1 = analysis1.getMappings().get(0);
        assertSame(step1, mapping1.getSource());
        assertSame(step2, mapping1.getTarget());

        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(1);
        assertEquals("otheranalysisid", analysis.getId());
        assertEquals("otheranalysisname", analysis.getName());
        assertEquals("otheranalysisdescription", analysis.getDescription());
        assertEquals("", analysis.getType());
        assertEquals(2, analysis.getSteps().size());
        assertEquals(0, analysis.getMappings().size());

        TransformationStep step3 = analysis.getSteps().get(0);
        assertEquals("stepid", step3.getGuid());
        assertEquals("stepname", step3.getName());
        assertEquals("stepdescription", step3.getDescription());

        Transformation transformation3 = step3.getTransformation();
        assertEquals("templateid", transformation3.getTemplate_id());
        assertEquals(2, transformation3.getPropertyValues().size());
        assertEquals("propertyvalue1", transformation3.getPropertyValues().get("propertyid1"));
        assertEquals("propertyvalue2", transformation3.getPropertyValues().get("propertyid2"));

        TransformationStep step4 = analysis.getSteps().get(1);
        assertEquals("otherstepid", step4.getGuid());
        assertEquals("otherstepname", step4.getName());
        assertEquals("otherstepdescription", step4.getDescription());

        Transformation transformation4 = step4.getTransformation();
        assertEquals("othertemplateid", transformation4.getTemplate_id());
        assertEquals(2, transformation4.getPropertyValues().size());
        assertEquals("othervalue1", transformation4.getPropertyValues().get("otherid1"));
        assertEquals("othervalue2", transformation4.getPropertyValues().get("otherid2"));
    }

    /**
     * Verifies that the analysis importer can handle a reference to a named template.
     * 
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    @Test
    public void shouldReferenceTemplateByName() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = createRegistry();
        importer.setRegistry(registry);

        JSONObject json = getTestJSONObject("should_reference_template_by_name");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(0);
        assertEquals(2, analysis.getSteps().size());
        assertEquals("fooid", analysis.getSteps().get(0).getTransformation().getTemplate_id());
        assertEquals("barid", analysis.getSteps().get(1).getTransformation().getTemplate_id());
    }

    /**
     * Verifies that the importer adds registered analyses to the analysis registry.
     * 
     * @throws JSONException
     */
    @Test
    public void shouldAddAnalysesToRegistry() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = createRegistry();
        importer.setRegistry(registry);

        JSONObject json = getTestJSONObject("should_add_analyses_to_register");
        importer.importObject(json);
        assertNotNull(registry.get(TransformationActivity.class, "analysisname"));
    }

    /**
     * Verifies that a reference to an unknown template name causes an exception.
     * 
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    @Test(expected = JSONException.class)
    public void unknownTemplateNameShouldCauseException() throws JSONException {
        HeterogeneousRegistryImpl registry = createRegistry();
        importer.setRegistry(registry);
        String jsonString = "{   \"analysis_id\": \"analysisid\",\n"
                + "    \"analysis_name\": \"analysisname\",\n"
                + "    \"description\": \"analysisdescription\",\n"
                + "    \"type\": \"analysistype\",\n"
                + "    \"steps\": [\n"
                + "        {   \"id\": \"stepid\",\n"
                + "            \"name\": \"stepname\",\n"
                + "            \"description\": \"stepdescription\",\n"
                + "            \"template_ref\": \"oof\",\n"
                + "            \"config\": {\n"
                + "                \"propertyid1\": \"propertyvalue1\",\n"
                + "                \"propertyid2\": \"propertyvalue2\",\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());
    }

    /**
     * Verifies that we can handle mixed template IDs and references in a single analysis.
     * 
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    @Test
    public void shouldHandleMixedTemplateIdsAndReferences() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = createRegistry();
        importer.setRegistry(registry);

        JSONObject json = getTestJSONObject("should_handle_mixed_template_ids_and_references");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(0);
        assertEquals(2, analysis.getSteps().size());
        assertEquals("templateid", analysis.getSteps().get(0).getTransformation().getTemplate_id());
        assertEquals("barid", analysis.getSteps().get(1).getTransformation().getTemplate_id());
    }

    /**
     * Verifies that the importer can handle references to named templates when importing a list of analyses.
     * 
     * @throws JSONException if the JSON array doesn't meet the importer's requirements.
     */
    @Test
    public void shouldHandleTemplateReferencesWhenImportingAnalysisList() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = createRegistry();
        importer.setRegistry(registry);

        JSONArray array = getTestJSONArray("should_handle_template_references_when_importing_analysis_list");
        importer.importObjectList(array);
        assertEquals(2, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis1 = getAnalysisDao().getSavedObjects().get(0);
        assertEquals(1, analysis1.getSteps().size());
        assertEquals("fooid", analysis1.getSteps().get(0).getTransformation().getTemplate_id());

        TransformationActivity analysis2 = getAnalysisDao().getSavedObjects().get(1);
        assertEquals(2, analysis2.getSteps().size());
        assertEquals("barid", analysis2.getSteps().get(0).getTransformation().getTemplate_id());
        assertEquals("othertemplateid", analysis2.getSteps().get(1).getTransformation().getTemplate_id());
    }

    /**
     * Verifies that analyses that are imported are added to the template group.
     * 
     * @throws JSONException if the JSON object doesn't meet the expectations of the importer.
     */
    @Test
    public void shouldAddAnalysisToTemplateGroup() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("should_add_analysis_to_template_group");
        importer.importObject(json);
        assertEquals(3, daoFactory.getMockTemplateGroupDao().getSavedObjects().size());
    }

    /**
     * Verifies that the importer can handle an empty transformation step configuration.
     * 
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    @Test
    public void shouldAcceptEmptyConfig() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("should_accept_empty_config");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());
    }

    /**
     * Validates that an analysis will not be imported if a vetted analysis
     * already exists.
     * 
     * An analysis is vetted if it appears in any template group other than "In Progress"
     *
     * @throws JSONException
     */
    @Test(expected = VettedWorkflowObjectException.class)
    public void doesNotStompVettedAnalysis() throws JSONException, IOException {
        TemplateGroup otherTemplateGroup = new TemplateGroup();
        otherTemplateGroup.setName("OtherGroup");
        otherTemplateGroup.setId("othergroup");
        otherTemplateGroup.setWorkspaceId(1);
        daoFactory.getMockTemplateGroupDao().save(otherTemplateGroup);

        JSONObject json = getTestJSONObject("does_not_stomp_vetted_analysis");
        importer.importObject(json);

        TransformationActivity analysis =
                daoFactory.getMockTransformationActivityDao().findUniqueInstanceByName("analysisname");
        otherTemplateGroup.addTemplate(analysis);

        // Make a small change to jsonString and re-import
        json = getTestJSONObject("does_not_stomp_vetted_analysis_changed");

        importer.enableReplacement();
        importer.importObject(json);
    }

    /**
     * This makes sure an unvetted analysis can be re-imported.
     *
     * @throws JSONException
     */
    @Test()
    public void multipleImportsWithUnvettedAnalysis() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("multiple_imports_with_unvetted_analysis");
        importer.importObject(json);

        // Make a small change to jsonString and re-import
        json = getTestJSONObject("multiple_imports_with_unvetted_analysis_changed");
        importer.enableReplacement();
        importer.importObject(json);
    }

    /**
     * Verifies that an analysis will not be replaced if updates are ignored.
     * 
     * @throws JSONException if a JSON error occurs.
     * @throws IOException if one of the test input files can't be read.
     */
    @Test
    public void shouldNotUpdateAnalysisWhenUpdatesAreDisabled() throws JSONException, IOException {
        importer.ignoreReplacement();
        JSONObject json = getTestJSONObject("multiple_imports_with_unvetted_analysis");
        importer.importObject(json);

        // Make a small change to jsonString and re-import
        json = getTestJSONObject("multiple_imports_with_unvetted_analysis_changed");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());
        assertEquals("analysisdescription", getAnalysisDao().getSavedObjects().get(0).getDescription());
    }

    /**
     * Verifies that the user's workspace is initialized.
     *
     * @throws JSONException if the JSON object doesn't meet the expectations of the importer.
     * @throws IOException if an I/O error occurs.
     */
    @Test()
    public void shouldInitializeWorkspace() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_analysis");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());
        assertEquals(1, daoFactory.getMockWorkspaceDao().getSavedObjects().size());

        Workspace workspace = daoFactory.getMockWorkspaceDao().getSavedObjects().get(0);
        assertNotNull(workspace.getRootAnalysisGroupId());

        TemplateGroup analysisGroup = daoFactory.getTemplateGroupDao().findByHid(workspace.getRootAnalysisGroupId());
        assertNotNull(analysisGroup);
        assertEquals(ROOT_ANALYSIS_GROUP, analysisGroup.getName());
        assertEquals(ANALYSIS_SUBGROUPS.size(), analysisGroup.getSub_groups().size());
        for (int i = 0; i < ANALYSIS_SUBGROUPS.size(); i++) {
            assertEquals(ANALYSIS_SUBGROUPS.get(i), analysisGroup.getSub_groups().get(i).getName());
        }
    }

	/**
	 * Verifies that an existing integration datum record will be used if one exists.
	 * 
	 * @throws JSONException if a JSON error occurs.
	 * @throws IOException if an I/O error occurs.
	 */
	@Test
	public void testExistingIntegrationDatum() throws JSONException, IOException {
		IntegrationDatum integrationDatum = new IntegrationDatum();
		integrationDatum.setId(new Long(247));
		integrationDatum.setIntegratorName("bob");
		integrationDatum.setIntegratorEmail("bob@bob-like.net");
		daoFactory.getIntegrationDatumDao().save(integrationDatum);

		JSONObject json = getTestJSONObject("minimally_specified_analysis");
        importer.importObject(json);
        assertEquals(1, getAnalysisDao().getSavedObjects().size());

        TransformationActivity analysis = getAnalysisDao().getSavedObjects().get(0);
		assertEquals(integrationDatum.getId(), analysis.getIntegrationDatum().getId());
	}

	/**
     * Creates a registry to use for testing and adds the templates in the registry to the mock Template DAO.
     * 
     * @return the registry.
     */
    private HeterogeneousRegistryImpl createRegistry() {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        for (Template template : registry.getRegisteredObjects(Template.class)) {
            daoFactory.getTemplateDao().save(template);
        }
        return registry;
    }
}
