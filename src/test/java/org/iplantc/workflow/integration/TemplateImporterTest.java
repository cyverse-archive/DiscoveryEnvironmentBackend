package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONObject;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.dao.mock.MockDataFormatDao;
import org.iplantc.workflow.dao.mock.MockDeployedComponentDao;
import org.iplantc.workflow.dao.mock.MockInfoTypeDao;
import org.iplantc.workflow.dao.mock.MockPropertyTypeDao;
import org.iplantc.workflow.dao.mock.MockRuleTypeDao;
import org.iplantc.workflow.dao.mock.MockTemplateDao;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InfoType;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
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
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.TemplateImporter.
 *
 * @author Dennis Roberts
 */
public class TemplateImporterTest {

    /**
     * The name of the user's root analysis group.
     */
    private static final String ROOT_ANALYSIS_GROUP = "Workspace";

    /**
     * The list of analysis subgroup names.
     */
    private static final List<String> ANALYSIS_SUBGROUPS = Arrays.asList("Dev", "Faves");

    /**
     * The factory used to generate mock data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The TemplateImporter instance that is being tested.
     */
    private TemplateImporter importer;

    /**
     * Reference to the object vetting analysis.
     */
    private MockAnalysisVetter analysisVetter;

    /**
     * The user service used to initialize the user's workspace.
     */
    private UserService userService;

    /**
     * The initializer that calls the user service.
     */
    private MockWorkspaceInitializer workspaceInitializer;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        daoFactory = new MockDaoFactory();
        initializePropertyTypeRetriever();
        initializeRuleTypeRetriever();
        initlizeMultiplicityDao();
        initializeInfoTypeRetriever();
        initializeDataFormatRetriever();
        initializeDeployedComponentDao();
        initializeUserService();
        initializeWorkspaceInitializer();
        UnitTestUtils.initializeDataSourceDao(daoFactory.getMockDataSourceDao());
        importer = new TemplateImporter(daoFactory);

        UnitTestUtils.addRootTemplateGroup(daoFactory.getMockTemplateGroupDao());
        analysisVetter = new MockAnalysisVetter();
        importer.setAnalysisVetter(analysisVetter);
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
    private void initlizeMultiplicityDao() {
        UnitTestUtils.initializeMultiplicityDao(daoFactory.getMockMultiplicityDao());
    }

    /**
     * Initializes the mock deployed component DAO for all unit tests.
     */
    private void initializeDeployedComponentDao() {
        MockDeployedComponentDao deployedComponentDao = daoFactory.getMockDeployedComponentDao();
        deployedComponentDao.save(UnitTestUtils.createDeployedComponent("component", "componentid"));
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
        // dataFormatRetriever.save(createDataFormat("qx/rhar"));
        // dataFormatRetriever.save(createDataFormat("gooberry"));
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
        // infoTypeRetriever.save(createInfoType("Quux"));
        // infoTypeRetriever.save(createInfoType("RaableRaable"));
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
        ruleType = new RuleType();
        ruleType.setName("IntAbove");
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
        propertyType.setDescription("propertytypedescription");
        propertyType.setHidable(true);
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("inputtypeid", "Input", "inputtypelabel", "inputtypedescription");
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("outputtypeid", "Output", "outputtypelabel",
                "outputtypedescription");
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("infotypeid", "Info", "infotypelabel", "infotypedescription");
        propertyTypeRetriever.save(propertyType);

        propertyType = new PropertyType("numbertypeid", "Number", "numbertypelabel",
                "numbertypedescription");
        propertyTypeRetriever.save(propertyType);
    }

    /**
     * Extracts the mock template DAO from the mock DAO factory.
     *
     * @return the mock template DAO.
     */
    private MockTemplateDao getMockTemplateDao() {
        return daoFactory.getMockTemplateDao();
    }

    /**
     * Extracts the mock data format DAO from the mock DAO factory.
     *
     * @return the mock data format DAO.
     */
    private MockDataFormatDao getMockDataFormatDao() {
        return daoFactory.getMockDataFormatDao();
    }

    /**
     * Extracts the mock property type DAO from the mock DAO factory.
     *
     * @return the mock property type DAO.
     */
    private MockPropertyTypeDao getMockPropertyTypeDao() {
        return daoFactory.getMockPropertyTypeDao();
    }

    @Test
    public void testFullySpecifiedTemplateWithAliasing() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template_alt_keys1");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
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
        assertTrue(input.isRequired());
        assertSame(getMockDataFormatDao().findByName("inputformat"), input.getDataFormat());

        input = template.getInputs().get(1);
        assertEquals("inprop2", input.getId());
        assertEquals("input2", input.getName());
        assertEquals("many", input.getMultiplicityName());
        assertEquals(1, input.getOrderd());
        assertEquals(" --input=", input.getSwitchString());
        assertEquals("inputtype", input.getInfoTypeName());
        assertTrue(input.isRequired());
        assertSame(getMockDataFormatDao().findByName("inputformat"), input.getDataFormat());

        PropertyGroup propertyGroup = template.getPropertyGroups().get(0);
        assertEquals("groupid", propertyGroup.getId());
        assertEquals("groupname", propertyGroup.getName());
        assertEquals("grouplabel", propertyGroup.getLabel());
        assertEquals("grouptype", propertyGroup.getGroupType());
        assertFalse(propertyGroup.isVisible());
        assertEquals(1, propertyGroup.getProperties().size());

        Property property = propertyGroup.getProperties().get(0);
        assertEquals("propertyid", property.getId());
        assertSame(getMockPropertyTypeDao().findUniqueInstanceByName("propertytypename"), property.getPropertyType());
        assertEquals("propertylabel", property.getLabel());
        assertEquals(0, property.getOrder());
        assertEquals("propertyvalue", property.getDefaultValue());
        assertFalse(property.getIsVisible());
        assertFalse(property.getOmitIfBlank());
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
        assertSame(getMockDataFormatDao().findByName("outputformat"), output.getDataFormat());
        assertEquals("outputformat", output.getDataFormat().getName());

        output = template.getOutputs().get(1);
        assertEquals("outprop2", output.getId());
        assertEquals("test.out", output.getName());
        assertEquals("single", output.getMultiplicityName());
        assertEquals(1, output.getOrderd());
        assertEquals("--out", output.getSwitchString());
        assertEquals("outputtype", output.getInfoTypeName());
        assertSame(getMockDataFormatDao().findByName("outputformat"), output.getDataFormat());
        assertEquals("outputformat", output.getDataFormat().getName());
    }

    /**
     * Verifies assumptions about the <code>order</code> key and assigning <code>order</code> given the properties
     * position in the property group list.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test
    public void testFullySpecifiedTemplateAssumedPropertyOrdering() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template_prop_order1");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template tmpl = getMockTemplateDao().getSavedObjects().get(0);
        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertEquals("templateid", template.getId());
        assertEquals("templatename", template.getName());
        assertEquals("templatelabel", template.getLabel());
        assertEquals("componentid", template.getComponent());
        assertEquals("templatetype", template.getTemplateType());
        assertEquals(2, template.getInputs().size());
        assertEquals(4, template.getPropertyGroups().size());
        assertEquals(2, template.getOutputs().size());

        PropertyGroup pgrp;
        Property prop;

        pgrp = tmpl.getPropertyGroups().get(0);
        assertEquals("group1", pgrp.getId());
        assertEquals("grpBASIL", pgrp.getName());
        assertEquals("BASIL", pgrp.getLabel());
        assertEquals("grouptype", pgrp.getGroupType());
        assertTrue(pgrp.isVisible());
        assertEquals(3, pgrp.getProperties().size());

        prop = pgrp.getProperties().get(0);
        assertEquals(0, prop.getOrder());
        assertEquals("650", prop.getDefaultValue());
        prop = pgrp.getProperties().get(1);
        assertEquals(1, prop.getOrder());
        assertEquals("651", prop.getDefaultValue());
        prop = pgrp.getProperties().get(2);
        assertEquals(2, prop.getOrder());
        assertEquals("652", prop.getDefaultValue());

        pgrp = tmpl.getPropertyGroups().get(1);
        assertEquals("group2", pgrp.getId());
        assertEquals("grpRAHR", pgrp.getName());
        assertEquals("RAHR", pgrp.getLabel());
        assertEquals("grouptype", pgrp.getGroupType());
        assertTrue(pgrp.isVisible());
        assertEquals(4, pgrp.getProperties().size());

        prop = pgrp.getProperties().get(0);
        assertEquals(0, prop.getOrder());
        assertEquals("650", prop.getDefaultValue());
        prop = pgrp.getProperties().get(1);
        assertEquals(99, prop.getOrder());
        assertEquals("651", prop.getDefaultValue());
        prop = pgrp.getProperties().get(2);
        assertEquals(2, prop.getOrder());
        assertEquals("652", prop.getDefaultValue());
        prop = pgrp.getProperties().get(3);
        assertEquals(187, prop.getOrder());
        assertEquals("653", prop.getDefaultValue());

    }

    /**
     * Verifies that we can import a fully specified template.
     *
     * @throws JSONException if the JSON object we pass in is invalid.
     */
    @Test
    public void testFullySpecifiedTemplate() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("fully_specified_template");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertEquals("templateid", template.getId());
        assertEquals("templatename", template.getName());
        assertEquals("templatelabel", template.getLabel());
        assertEquals("componentid", template.getComponent());
        assertEquals("templatetype", template.getTemplateType());
        assertEquals(2, template.getInputs().size());
        assertEquals(3, template.getPropertyGroups().size());
        assertEquals(1, template.getOutputs().size());

        DataObject input1 = template.getInputs().get(0);
        assertEquals("inprop1", input1.getId());
        assertEquals("inputname", input1.getName());
        assertEquals("single", input1.getMultiplicityName());
        assertEquals(27, input1.getOrderd());
        assertEquals("--foo", input1.getSwitchString());
        assertEquals("inputtype", input1.getInfoTypeName());
        assertTrue(input1.isRequired());
        assertSame(getMockDataFormatDao().findByName("inputformat"), input1.getDataFormat());

        DataObject input2 = template.getInputs().get(1);
        assertEquals("inprop2", input2.getId());
        assertEquals("otherinputname", input2.getName());
        assertEquals("single", input2.getMultiplicityName());
        assertEquals(57, input2.getOrderd());
        assertEquals("--bar", input2.getSwitchString());
        assertEquals("inputtype", input2.getInfoTypeName());
        assertFalse(input2.isRequired());
        assertSame(getMockDataFormatDao().findByName("inputformat"), input2.getDataFormat());

        PropertyGroup propertyGroup = template.getPropertyGroups().get(0);
        assertEquals("groupid", propertyGroup.getId());
        assertEquals("groupname", propertyGroup.getName());
        assertEquals("grouplabel", propertyGroup.getLabel());
        assertEquals("grouptype", propertyGroup.getGroupType());
        assertFalse(propertyGroup.isVisible());
        assertEquals(1, propertyGroup.getProperties().size());

        Property property = propertyGroup.getProperties().get(0);
        assertEquals("propertyid", property.getId());
        assertEquals("propertyname", property.getName());
        assertSame(getMockPropertyTypeDao().findUniqueInstanceByName("propertytypename"), property.getPropertyType());
        assertEquals("propertylabel", property.getLabel());
        assertEquals(57, property.getOrder());
        assertEquals("propertyvalue", property.getDefaultValue());
        assertFalse(property.getIsVisible());
        assertTrue(property.getOmitIfBlank());
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
        assertEquals(11, output.getOrderd());
        assertEquals("--bar", output.getSwitchString());
        assertEquals("outputtype", output.getInfoTypeName());
        assertSame(getMockDataFormatDao().findByName("outputformat"), output.getDataFormat());
        assertEquals("outputformat", output.getDataFormat().getName());
    }

    /**
     * Verifies that we can import a minimally specified template.
     *
     * @throws JSONException if the JSON that is given to the importer is invalid.
     * @throws IOException if the file is not present at the specified path.
     */
    @Test
    public void testMinimallySpecifiedTemplate() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("minimally_specified_template");
        verifyMinimallySpecifiedTemplate(json);
    }

    private void createReferenceAnalysis() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("reference_analysis");

        AnalysisImporter analysisImporter =
                new AnalysisImporter(daoFactory, new TemplateGroupImporter(daoFactory, 0, 1), workspaceInitializer);
        analysisImporter.importObject(json);
    }

    /**
     * Verifies that a vetted Template cannot be replaced by a new template.
     *
     * @throws JSONException if the JSON that is given to the importer is invalid.
     * @throws IOException if the file is not present at the specified path.
     */
    @Test(expected = VettedWorkflowObjectException.class)
    public void testRejectingVettedTemplate() throws JSONException, IOException {
        analysisVetter.setObjectVetted(true);
        importer.enableReplacement();

        JSONObject json = getTestJSONObject("minimally_specified_template_with_id");
        importer.importObject(json);

        createReferenceAnalysis();

        // Re-import to trigger vetting
        importer.importObject(json);
    }

    /**
     * Verifies that a unvetted Template can be replaced by a new template.
     *
     * @throws JSONException if the JSON that is given to the importer is invalid.
     * @throws IOException if the file is not present at the specified path.
     */
    @Test()
    public void testUnvettedTemplateReplacement() throws JSONException, IOException {
        analysisVetter.setObjectVetted(false);
        importer.enableReplacement();

        JSONObject json = getTestJSONObject("minimally_specified_template_with_id");
        importer.importObject(json);

        createReferenceAnalysis();

        // Re-import to trigger vetting
        importer.importObject(json);
    }

    /**
     *
     */
    @Test
    public void testTemplateReplaceWithNoAssociatedAnalyses() throws JSONException, IOException {
        importer.enableReplacement();

        JSONObject json = getTestJSONObject("minimally_specified_template_with_id");
        importer.importObject(json);

        // Re-import to trigger vetting
        importer.importObject(json);
    }

    /**
     * Verifies that templates are not updated if we've instructed the importer to ignore attempts to replace
     * existing templates.
     *
     * @throws JSONException if a JSON error occurs.
     * @throws IOException if we try to load the JSON from a non-existent file.
     */
    @Test
    public void templateShouldNotBeUpdatedIfReplacementIgnored() throws JSONException, IOException {
        importer.ignoreReplacement();

        JSONObject json1 = getTestJSONObject("minimally_specified_template_with_id");
        importer.importObject(json1);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());
        assertEquals("", getMockTemplateDao().getSavedObjects().get(0).getName());

        JSONObject json2 = getTestJSONObject("minimally_specified_template_with_id_2");
        importer.importObject(json2);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());
        assertEquals("", getMockTemplateDao().getSavedObjects().get(0).getName());
    }

    /**
     * Verifies that we can import a minimally specified template with alternative key names.
     *
     * @throws JSONException if the JSON that is given to the importer is invalid.
     * @throws IOException if the file is not present at the specified path.
     */
    @Test
    public void testMinimallySpecifiedTemplateAlternativeKeys() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("minimally_specified_template_alt_keys1");
        verifyMinimallySpecifiedTemplate(json);
    }

    private void verifyMinimallySpecifiedTemplate(JSONObject json) throws JSONException, IOException {
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertTrue(template.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", template.getName());
        assertEquals("componentid", template.getComponent());
        assertEquals("templatetype", template.getTemplateType());
        assertEquals(1, template.getInputs().size());
        assertEquals(1, template.getPropertyGroups().size());
        assertEquals(1, template.getOutputs().size());

        DataObject input = template.getInputs().get(0);
        assertEquals("inputname", input.getName());
        assertEquals("single", input.getMultiplicityName());
        assertEquals(0, input.getOrderd());
        assertEquals("", input.getSwitchString());
        assertEquals("inputtype", input.getInfoTypeName());
        assertTrue(input.isRequired());
        assertSame(getMockDataFormatDao().findByName("Unspecified"), input.getDataFormat());
        assertEquals("", input.getDescription());

        PropertyGroup propertyGroup = template.getPropertyGroups().get(0);
        assertTrue(propertyGroup.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", propertyGroup.getName());
        assertEquals("", propertyGroup.getLabel());
        assertEquals("grouptype", propertyGroup.getGroupType());
        assertTrue(propertyGroup.isVisible());
        assertEquals(3, propertyGroup.getProperties().size());

        Property property = propertyGroup.getProperties().get(1);
        assertTrue(property.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", property.getName());
        assertSame(getMockPropertyTypeDao().findUniqueInstanceByName("propertytypename"), property.getPropertyType());
        assertEquals("", property.getLabel());
        assertEquals("", property.getDescription());
        assertEquals(1, property.getOrder());
        assertNull(property.getDefaultValue());
        assertTrue(property.getIsVisible());
        assertTrue(property.getOmitIfBlank());
        assertNotNull(property.getValidator());

        Validator validator = property.getValidator();
        assertTrue(validator.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", validator.getName());
        assertFalse(validator.isRequired());
        assertEquals(1, validator.getRules().size());

        Rule rule = validator.getRules().get(0);
        assertEquals("ruletype", rule.getRuleType().getName());
        assertEquals(2, rule.getArguments().size());
        assertEquals("rulearg1", rule.getArguments().get(0));
        assertEquals("rulearg2", rule.getArguments().get(1));

        DataObject output = template.getOutputs().get(0);
        assertEquals("outputname", output.getName());
        assertEquals("many", output.getMultiplicityName());
        assertEquals(2, output.getOrderd());
        assertEquals("", output.getSwitchString());
        assertEquals("outputtype", output.getInfoTypeName());
        assertSame(getMockDataFormatDao().findByName("Unspecified"), output.getDataFormat());
        assertEquals("", output.getDescription());
    }

    /**
     * Verifies that we can test a template with no inputs.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testTemplateWithNoInputs() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("template_with_no_inputs");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertTrue(template.getId().matches("[-0-9A-F]{36}"));
        assertEquals(0, template.getInputs().size());
    }

    /**
     * Verifies that we can import a template with no outputs.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testTemplateWithNoOutputs() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("template_with_no_outputs");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertEquals(0, template.getOutputs().size());
    }

    /**
     * Verifies that we can test a template with no outputs.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testTemplateLabelShouldBeSetToTemplateNameIfNotProvided() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("template_label_should_be_set_to_template_name");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertEquals("templatename", template.getLabel());
    }

    /**
     * Verifies that a missing list of property groups generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test(expected = JSONException.class)
    public void testTemplateWithNoPropertyGroups() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("template_with_no_property_groups");
        importer.importObject(json);
    }

    /**
     * Verifies that a missing list of properties generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test(expected = JSONException.class)
    public void testPropertyGroupWithNoProperties() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("property_group_with_no_properties");
        importer.importObject(json);
    }

    /**
     * Verifies that we can handle a property with no validator.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testPropertyWithNoValidator() throws JSONException {
        String jsonString = "{   \"component\": \"componentid\",\n"
                + "    \"type\": \"templatetype\",\n"
                + "    \"groups\": [\n"
                + "        {    \"type\": \"grouptype\",\n"
                + "             \"properties\": [\n"
                + "                 {   \"type\": \"propertytypename\"\n"
                + "                 }\n"
                + "             ]\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());

        Template template = getMockTemplateDao().getSavedObjects().get(0);
        assertEquals(1, template.getPropertyGroups().size());

        PropertyGroup propertyGroup = template.getPropertyGroups().get(0);
        assertEquals(1, propertyGroup.getProperties().size());

        Property property = propertyGroup.getProperties().get(0);
        assertNull(property.getValidator());
    }

    /**
     * Verifies that the importer can handle a validator with a missing rule list.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testValidatorWithNoRules() throws JSONException {
        String jsonString = "{   \"component\": \"componentid\",\n"
                + "    \"type\": \"templatetype\",\n"
                + "    \"groups\": [\n"
                + "        {    \"type\": \"grouptype\",\n"
                + "             \"properties\": [\n"
                + "                 {   \"type\": \"propertytypename\",\n"
                + "                     \"validator\": {\n"
                + "                         \"name\": \"validatorname\"\n"
                + "                     }\n"
                + "                 }\n"
                + "             ]\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());
    }

    /**
     * Verifies that a validator with badly formatted rule definition generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test(expected = JSONException.class)
    public void testInvalidRuleSpecification() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("invalid_rule_specification");
        importer.importObject(json);
    }

    /**
     * Verifies that a template with no component generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test()
    public void testTemplateWithNoComponent() throws JSONException {
        String jsonString = "{   \"type\": \"templatetype\",\n"
                + "    \"groups\": [\n"
                + "        {    \"type\": \"grouptype\",\n"
                + "             \"properties\": [\n"
                + "                 {   \"type\": \"propertytypename\"\n"
                + "                 }\n"
                + "             ]\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a template with a missing type generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testTemplateWithNoType() throws JSONException {
        String jsonString = "{   \"component\": \"componentid\",\n"
                + "    \"groups\": [\n"
                + "        {    \"type\": \"grouptype\",\n"
                + "             \"properties\": [\n"
                + "                 {   \"type\": \"propertytypename\"\n"
                + "                 }\n"
                + "             ]\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());
        assertEquals("", getMockTemplateDao().getSavedObjects().get(0).getTemplateType());
    }

    /**
     * Verifies that a property group with no type generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test
    public void testPropertyGroupWithNoType() throws JSONException {
        String jsonString = "{   \"component\": \"componentid\",\n"
                + "    \"groups\": [\n"
                + "        {    \"properties\": [\n"
                + "                 {   \"type\": \"propertytypename\"\n"
                + "                 }\n"
                + "             ]\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals("", getMockTemplateDao().getSavedObjects().get(0).getPropertyGroups().get(0).getGroupType());
    }

    /**
     * Verifies that a property with no type generates an exception.
     *
     * @throws JSONException if the JSON we pass to the importer is invalid.
     */
    @Test(expected = JSONException.class)
    public void testPropertyWithNoType() throws JSONException {
        String jsonString = "{   \"component\": \"componentid\",\n"
                + "    \"groups\": [\n"
                + "        {    \"type\": \"grouptype\",\n"
                + "             \"properties\": [ {} ]\n"
                + "        }\n"
                + "    ]\n"
                + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that the importer can import multiple templates.
     *
     * @throws JSONException if the JSON object we pass to the importer doesn't meet the requirements.
     */
    @Test
    public void testMultipleTemplates() throws JSONException, IOException {
        JSONArray json = getTestJSONArray("multiple_templates");
        importer.importObjectList(json);
        assertEquals(2, getMockTemplateDao().getSavedObjects().size());

        Template template1 = getMockTemplateDao().getSavedObjects().get(0);
        assertEquals("templateid", template1.getId());
        assertEquals("templatename", template1.getName());
        assertEquals("templatelabel", template1.getLabel());
        assertEquals("componentid", template1.getComponent());
        assertEquals("templatetype", template1.getTemplateType());
        assertEquals(1, template1.getInputs().size());
        assertEquals(1, template1.getOutputs().size());

        DataObject input1 = template1.getInputs().get(0);
        assertEquals("inprop1", input1.getId());
        assertEquals("inputname", input1.getName());
        assertEquals("single", input1.getMultiplicityName());
        assertEquals(27, input1.getOrderd());
        assertEquals("--foo", input1.getSwitchString());
        assertEquals("inputtype", input1.getInfoTypeName());
        assertTrue(input1.isRequired());
        assertEquals(1, template1.getPropertyGroups().size());

        PropertyGroup propertyGroup1 = template1.getPropertyGroups().get(0);
        assertEquals("groupid", propertyGroup1.getId());
        assertEquals("groupname", propertyGroup1.getName());
        assertEquals("grouplabel", propertyGroup1.getLabel());
        assertEquals("grouptype", propertyGroup1.getGroupType());
        assertFalse(propertyGroup1.isVisible());
        assertEquals(3, propertyGroup1.getProperties().size());

        Property property1 = propertyGroup1.getProperties().get(1);
        assertEquals("propertyid", property1.getId());
        assertEquals("propertyname", property1.getName());
        assertSame(getMockPropertyTypeDao().findUniqueInstanceByName("propertytypename"), property1.getPropertyType());
        assertEquals("propertylabel", property1.getLabel());
        assertEquals(57, property1.getOrder());
        assertEquals("propertyvalue", property1.getDefaultValue());
        assertFalse(property1.getIsVisible());
        assertNotNull(property1.getValidator());

        Validator validator1 = property1.getValidator();
        assertEquals("validatorid", validator1.getId());
        assertEquals("validatorname", validator1.getName());
        assertFalse(validator1.isRequired());
        assertEquals(1, validator1.getRules().size());

        Rule rule1 = validator1.getRules().get(0);
        assertEquals("ruletype", rule1.getRuleType().getName());
        assertEquals(2, rule1.getArguments().size());
        assertEquals("rulearg1", rule1.getArguments().get(0));
        assertEquals("rulearg2", rule1.getArguments().get(1));

        DataObject output1 = template1.getOutputs().get(0);
        assertEquals("outprop1", output1.getId());
        assertEquals("outputname", output1.getName());
        assertEquals("single", output1.getMultiplicityName());
        assertEquals(11, output1.getOrderd());
        assertEquals("--bar", output1.getSwitchString());
        assertEquals("outputtype", output1.getInfoTypeName());
        assertEquals(1, template1.getPropertyGroups().size());

        Template template2 = getMockTemplateDao().getSavedObjects().get(1);
        assertTrue(template2.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", template2.getName());
        assertEquals("componentid", template2.getComponent());
        assertEquals("templatetype", template2.getTemplateType());
        assertEquals(1, template2.getInputs().size());
        assertEquals(1, template2.getOutputs().size());

        DataObject input2 = template2.getInputs().get(0);
        assertEquals("inprop1", input2.getId());
        assertEquals("inputname", input2.getName());
        assertEquals("many", input2.getMultiplicityName());
        assertEquals(0, input2.getOrderd());
        assertEquals("", input2.getSwitchString());
        assertEquals("inputtype", input2.getInfoTypeName());
        assertTrue(input2.isRequired());
        assertEquals(1, template2.getPropertyGroups().size());

        PropertyGroup propertyGroup2 = template2.getPropertyGroups().get(0);
        assertTrue(propertyGroup2.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", propertyGroup2.getName());
        assertEquals("", propertyGroup2.getLabel());
        assertEquals("grouptype", propertyGroup2.getGroupType());
        assertTrue(propertyGroup2.isVisible());
        assertEquals(3, propertyGroup2.getProperties().size());

        Property property2 = propertyGroup2.getProperties().get(1);
        assertTrue(property2.getId().matches("[-0-9A-F]{36}"));
        assertEquals("", property2.getName());
        assertSame(getMockPropertyTypeDao().findUniqueInstanceByName("propertytypename"), property2.getPropertyType());
        assertEquals("", property2.getLabel());
        assertEquals(1, property2.getOrder());
        assertNull(property2.getDefaultValue());
        assertTrue(property2.getIsVisible());
        assertNotNull(property2.getValidator());

        Validator validator2 = property2.getValidator();
        assertTrue(validator2.getId().matches("[-0-9A-F]{36}"));
        assertEquals("validatorname", validator2.getName());
        assertFalse(validator2.isRequired());
        assertEquals(1, validator2.getRules().size());

        Rule rule2 = validator2.getRules().get(0);
        assertEquals("ruletype", rule2.getRuleType().getName());
        assertEquals(2, rule2.getArguments().size());
        assertEquals("rulearg1", rule2.getArguments().get(0));
        assertEquals("rulearg2", rule2.getArguments().get(1));

        DataObject output2 = template2.getOutputs().get(0);
        assertEquals("outprop1", output2.getId());
        assertEquals("outputname", output2.getName());
        assertEquals("many", output2.getMultiplicityName());
        assertEquals(2, output2.getOrderd());
        assertEquals("", output2.getSwitchString());
        assertEquals("outputtype", output2.getInfoTypeName());
        assertEquals(1, template2.getPropertyGroups().size());
    }

    /**
     * Verifies that we can handle deployed component references.
     *
     * @throws JSONException if the JSON object doesn't meet the expectations of the importer.
     */
    @Test
    public void testNamedComponentReferences() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        JSONObject json = getTestJSONObject("named_component_references");
        importer.importObject(json);
        assertEquals(1, getMockTemplateDao().getSavedObjects().size());
    }

    /**
     * Verifies that the importer will register templates if a registry is specified.
     *
     * @throws JSONException if the JSON we pass to the importer doesn't meet the importer's requirements.
     */
    @Test
    public void testShouldRegisterTemplate() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        JSONObject json = getTestJSONObject("should_register_template");
        importer.importObject(json);
        assertEquals(3, registry.size(Template.class));
        assertNotNull(registry.get(Template.class, "templatename"));
    }

    /**
     * Verifies that the importer will register multiple templates.
     *
     * @throws JSONException if the JSON we pass to the importer doesn't meet the requirements.
     */
    @Test
    public void shouldRegisterMultipleTemplates() throws JSONException, IOException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        JSONArray json = getTestJSONArray("should_register_multiple_templates");
        importer.importObjectList(json);
        assertEquals(4, registry.size(Template.class));
        assertNotNull(registry.get(Template.class, "templatename"));
        assertNotNull(registry.get(Template.class, "emanetalpmet"));
    }

    /**
     * Verifies that an unknown rule type causes an exception to be thrown.
     *
     * @throws JSONException if the JSON document doesn't meet the requirements of the importer.
     */
    @Test(expected = JSONException.class)
    public void testUnknownRuleType() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("unknown_rule_type");
        importer.importObject(json);
    }

    /**
     * Verifies that we can import a template with an informational property.
     *
     * @throws JSONException if the JSON is invalid or doesn't meet the expectations of the importer.
     * @throws IOException if the JSON string can't be read from the file.
     */
    @Test
    public void testInfoParameter() throws JSONException, IOException {
        JSONObject json = getTestJSONObject("template_with_info_parameter");
        importer.importObject(json);
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
}
