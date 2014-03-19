package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.dao.mock.MockDataFormatDao;
import org.iplantc.workflow.dao.mock.MockInfoTypeDao;
import org.iplantc.workflow.dao.mock.MockPropertyTypeDao;
import org.iplantc.workflow.dao.mock.MockRuleTypeDao;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InfoType;
import org.iplantc.workflow.integration.validation.TemplateValidator;
import org.iplantc.workflow.integration.validation.TemplateValidatorFactory;
import org.iplantc.workflow.integration.validation.TooManyOutputRedirectionsException;
import org.iplantc.workflow.mock.MockWorkspaceInitializer;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.PropertyType;
import org.iplantc.workflow.model.Rule;
import org.iplantc.workflow.model.RuleType;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.model.Validator;
import org.iplantc.workflow.service.UserService;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.AnalysisGeneratingTemplateImporter.
 * 
 * @author Dennis Roberts
 */
public class AnalysisGeneratingTemplateImporterTest {

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
     * The mock data access object factory.
     */
    private MockDaoFactory daoFactory;

    /**
     * Used to add generated analyses to the default template group.
     */
    private TemplateGroupImporter templateGroupImporter;

    /**
     * The TemplateImporter instance that is being tested.
     */
    private AnalysisGeneratingTemplateImporter importer;

    /**
     * The user service used to initialize the user's workspace.
     */
    private UserService userService;

    /**
     * The initializer that calls the user service.
     */
    private MockWorkspaceInitializer workspaceInitializer;

    /**
     * The validator to use when importing templates.
     */
    private TemplateValidator templateValidator = TemplateValidatorFactory.createDefaultTemplateValidator();

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        daoFactory = new MockDaoFactory();
        initializePropertyTypeRetriever();
        initializeRuleTypeRetriever();
        initializeInfoTypeRetriever();
        initializeMultiplicityDao();
        initializeDataFormatRetriever();
        initializeDataSourceDao();
        initializeDeployedComponentDao();
        initializeTemplateGroupDao();
        initializeTemplateGroupImporter();
        initializeUserService();
        initializeWorkspaceInitializer();
        importer = new AnalysisGeneratingTemplateImporter(daoFactory, templateGroupImporter, workspaceInitializer,
                templateValidator);
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
     * Initializes the multiplicity DAO for all unit tests.
     */
    private void initializeMultiplicityDao() {
        UnitTestUtils.initializeMultiplicityDao(daoFactory.getMockMultiplicityDao());
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
     * Initializes the mock template group DAO for all unit tests.
     */
    private void initializeTemplateGroupDao() {
        UnitTestUtils.addRootTemplateGroup(daoFactory.getMockTemplateGroupDao());
    }

    /**
     * Initializes the deployed component DAO for all unit tests.
     */
    private void initializeDeployedComponentDao() {
        daoFactory.getDeployedComponentDao().save(UnitTestUtils.createDeployedComponent("component", "componentid"));
    }

    /**
     * Initializes the mock data format retriever for all unit tests.
     */
    private void initializeDataFormatRetriever() {
        MockDataFormatDao dataFormatRetriever = daoFactory.getMockDataFormatDao();
        dataFormatRetriever.save(createDataFormat("inputformat"));
        dataFormatRetriever.save(createDataFormat("input-ndy"));
        dataFormatRetriever.save(createDataFormat("outputformat"));
        dataFormatRetriever.save(createDataFormat("Unspecified"));
    }

    /**
     * Initializes the mock data source DAO for all unit tests.
     */
    private void initializeDataSourceDao() {
        UnitTestUtils.initializeDataSourceDao(daoFactory.getMockDataSourceDao());
    }

    /**
     * Creates a data format with the given name.
     * 
     * @param name the name of the data format.
     * @return the data format.
     */
    private DataFormat createDataFormat(String name) {
        return new DataFormat(name + "id", name, name + "label");
    }

    /**
     * Initializes the info type retriever.
     */
    private void initializeInfoTypeRetriever() {
        MockInfoTypeDao infoTypeRetriever = daoFactory.getMockInfoTypeDao();
        infoTypeRetriever.save(createInfoType("inputtype"));
        infoTypeRetriever.save(createInfoType("outputtype"));
    }

    /**
     * Creates an info type with the given name.
     * 
     * @param name the name of the info type.
     * @return the info type.
     */
    private InfoType createInfoType(String name) {
        return new InfoType(name + "id", name, name + "label", name + "description");
    }

    /**
     * Initializes the rule type retriever.
     */
    private void initializeRuleTypeRetriever() {
        MockRuleTypeDao ruleTypeRetriever = daoFactory.getMockRuleTypeDao();
        RuleType ruleType = new RuleType();
        ruleType.setId("ruletypeid");
        ruleType.setName("ruletype");
        ruleType.setLabel("ruletypelabel");
        ruleType.setDescription("ruletypedescription");
        ruleTypeRetriever.save(ruleType);
    }

    /**
     * Initializes the property type retriever.
     */
    private void initializePropertyTypeRetriever() {
        MockPropertyTypeDao propertyTypeRetriever = daoFactory.getMockPropertyTypeDao();
        PropertyType propertyType = new PropertyType();
        propertyType.setId("propertytypeid");
        propertyType.setName("propertytypename");
        propertyType.setLabel("propertytypelabel");
        propertyType.setHidable(true);
        propertyType.setDescription("propertytypedescription");
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("inputtypeid", "Input", "inputtypelabel", "inputtypedescription");
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("outputtypeid", "Output", "outputtypelabel",
                "outputtypedescription");
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("infotypeid", "Info", "infotypelabel", "infotypedescription");
        propertyTypeRetriever.save(propertyType);
    }

    /**
     * Verifies that analyses are automatically generated and added to the default template group. Some other assertions
     * are also done to make sure that the base class is called correctly.
     * 
     * @throws JSONException if the JSON object doesn't satisfy the importer's requirements.
     * @throws IOException if the
     */
    @Test
    public void testAutomaticAnalysisGeneration() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template_alt_keys1");
        importer.importObject(json);
        assertEquals(1, getSavedTemplates().size());
        assertEquals(1, getSavedAnalyses().size());
        assertEquals(3, getSavedTemplateGroups().size());

        Template template = getSavedTemplates().get(0);
        assertEquals("templateid", template.getId());
        assertEquals("templatename", template.getName());
        assertEquals("templatelabel", template.getLabel());
        assertEquals("componentid", template.getComponent());
        assertEquals("templatetype", template.getTemplateType());
        assertEquals(2, template.getInputs().size());
        assertEquals(3, template.getPropertyGroups().size());
        assertEquals(2, template.getOutputs().size());

        DataObject input = template.getInputs().get(0);
        assertEquals("inprop1", input.getId());
        assertEquals("input1", input.getName());
        assertEquals("single", input.getMultiplicityName());
        assertEquals(0, input.getOrderd());
        assertEquals("--foo", input.getSwitchString());
        assertEquals("inputtype", input.getInfoTypeName());
        assertSame(getDataFormat("inputformat"), input.getDataFormat());

        input = template.getInputs().get(1);
        assertEquals("inprop2", input.getId());
        assertEquals("input2", input.getName());
        assertEquals("many", input.getMultiplicityName());
        assertEquals(1, input.getOrderd());
        assertEquals(" --input=", input.getSwitchString());
        assertEquals("inputtype", input.getInfoTypeName());
        assertSame(getDataFormat("inputformat"), input.getDataFormat());

        PropertyGroup propertyGroup = template.getPropertyGroups().get(0);
        assertEquals("groupid", propertyGroup.getId());
        assertEquals("groupname", propertyGroup.getName());
        assertEquals("grouplabel", propertyGroup.getLabel());
        assertEquals("grouptype", propertyGroup.getGroupType());
        assertFalse(propertyGroup.isVisible());
        assertEquals(1, propertyGroup.getProperties().size());

        Property property = propertyGroup.getProperties().get(0);
        assertEquals("propertyid", property.getId());
        assertSame(getPropertyType("propertytypename"), property.getPropertyType());
        assertEquals("propertylabel", property.getLabel());
        assertEquals(0, property.getOrder());
        assertEquals("propertyvalue", property.getDefaultValue());
        assertFalse(property.getIsVisible());
        assertNotNull(property.getValidator());

        Validator validator = property.getValidator();
        assertEquals("validatorid", validator.getId());
        assertEquals("validatorname", validator.getName());
        assertFalse(validator.isRequired());
        assertEquals(1, validator.getRules().size());

        Rule rule = validator.getRules().get(0);
        assertEquals("ruletype", rule.getRuleType().getName());
        assertEquals(2, rule.getArguments().size());
        assertEquals("rulearg1", rule.getArguments().get(0));
        assertEquals("rulearg2", rule.getArguments().get(1));

        DataObject output = template.getOutputs().get(0);
        assertEquals("outprop1", output.getId());
        assertEquals("test.out", output.getName());
        assertEquals("single", output.getMultiplicityName());
        assertEquals(0, output.getOrderd());
        assertEquals("--goo", output.getSwitchString());
        assertEquals("outputtype", output.getInfoTypeName());
        assertSame(getDataFormat("outputformat"), output.getDataFormat());
        assertEquals("outputformat", output.getDataFormat().getName());

        output = template.getOutputs().get(1);
        assertEquals("outprop2", output.getId());
        assertEquals("test.out", output.getName());
        assertEquals("single", output.getMultiplicityName());
        assertEquals(1, output.getOrderd());
        assertEquals("--out", output.getSwitchString());
        assertEquals("outputtype", output.getInfoTypeName());
        assertSame(getDataFormat("outputformat"), output.getDataFormat());
        assertEquals("outputformat", output.getDataFormat().getName());

        TransformationActivity analysis = getSavedAnalyses().get(0);
        assertEquals("templatename", analysis.getName());
    }

    /**
     * Verifies that the importer initializes the user's workspace.
     *
     * @throws JSONException if a JSON error occurs.
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void shouldInitializeWorkspace() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template_alt_keys1");
        importer.importObject(json);
        assertEquals(1, daoFactory.getMockTransformationActivityDao().getSavedObjects().size());
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

        JSONObject json = getTestJSONObject("fully_specified_template_alt_keys1");
        importer.importObject(json);
		
        assertEquals(1, getSavedTemplates().size());
        assertEquals(1, getSavedAnalyses().size());
        assertEquals(3, getSavedTemplateGroups().size());

		TransformationActivity analysis = getSavedAnalyses().get(0);
		assertEquals(integrationDatum.getId(), analysis.getIntegrationDatum().getId());
	}

    /**
     * Verifies that the validation fails if there are multiple redirections to standard output.
     * 
     * @throws JSONException if a JSON error occurs.
     * @throws IOException if an I/O error occurs.
     */
    @Test(expected = TooManyOutputRedirectionsException.class)
    public void testMultipleStdoutRedirections() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template_multiple_stdout_redirections");
        importer.importObject(json);
    }

    /**
     * Verifies that the validation fails if there are multiple redirections to standard error output.
     * 
     * @throws JSONException if a JSON error occurs.
     * @throws IOException if an I/O error occurs.
     */
    @Test(expected = TooManyOutputRedirectionsException.class)
    public void testMultipleStderrRedirections() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template_multiple_stderr_redirections");
        importer.importObject(json);
    }

    /**
     * @param name the name of the property type.
     * @return the property type with the given name or null if no matching property type is found.
     */
    private Object getPropertyType(String name) {
        return daoFactory.getPropertyTypeDao().findUniqueInstanceByName(name);
    }

    /**
     * @param inputFormat the name of the input format.
     * @return the input format name or null if no matching input format is found.
     */
    protected DataFormat getDataFormat(String inputFormat) {
        return daoFactory.getDataFormatDao().findByName(inputFormat);
    }

    /**
     * @return the list of template groups that have been saved.
     */
    protected List<TemplateGroup> getSavedTemplateGroups() {
        return daoFactory.getMockTemplateGroupDao().getSavedObjects();
    }

    /**
     * @return the list of analyses that have been saved.
     */
    protected List<TransformationActivity> getSavedAnalyses() {
        return daoFactory.getMockTransformationActivityDao().getSavedObjects();
    }

    /**
     * @return the list of templates that have been saved.
     */
    protected List<Template> getSavedTemplates() {
        return daoFactory.getMockTemplateDao().getSavedObjects();
    }
}
