package org.iplantc.workflow.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dao.components.ToolTypeDao;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.persistence.dto.components.ToolType;
import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.persistence.dto.data.DataSource;
import org.iplantc.persistence.dto.data.DeployedComponentDataFile;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.UnknownToolTypeException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.core.TransformationActivityReference;
import org.iplantc.workflow.dao.mock.MockDataFormatDao;
import org.iplantc.workflow.dao.mock.MockDataSourceDao;
import org.iplantc.workflow.dao.mock.MockInfoTypeDao;
import org.iplantc.workflow.dao.mock.MockMultiplicityDao;
import org.iplantc.workflow.dao.mock.MockPropertyTypeDao;
import org.iplantc.workflow.dao.mock.MockRuleTypeDao;
import org.iplantc.workflow.dao.mock.MockTemplateGroupDao;
import org.iplantc.workflow.data.InfoType;
import org.iplantc.workflow.data.Multiplicity;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.model.PropertyType;
import org.iplantc.workflow.model.RuleType;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.template.groups.TemplateGroup;

/**
 * Various utility methods to facilitate unit and functional testing.
 *
 * @author Dennis Roberts
 */
public class UnitTestUtils {

    /**
     * Prevent instantiation.
     */
    private UnitTestUtils() {}

    /**
     * The list of standard property type names.
     */
    private static final String[] PROPERTY_TYPE_NAMES = {"ClipperSelector", "Script", "Mode", "BarcodeSelector",
        "TNRSFileSelector", "Text", "Number", "QuotedText", "XBasePairs", "Flag", "Selection", "ValueSelection",
        "Info"};

    /**
     * The list of standard rule type names.
     */
    private static final String[] RULE_TYPE_NAMES = {"IntBelowField", "IntAbove", "IntRange", "IntAboveField",
        "MustContain", "DoubleRange", "IntBelow"};

    /**
     * The list of standard info type names.
     */
    private static final String[] INFO_TYPE_NAMES = {"ReconcileTaxa", "ReferenceGenome", "ReferenceSequence",
        "ReferenceAnnotation", "ACEField", "ContrastField", "ReferenceDummyGenes", "File"};

    /**
     * The list of standard data format names.
     */
    private static final String[] DATA_FORMAT_NAMES = {"ASN-0", "Barcode-0", "CSV-0", "EMBL-0", "FAI-0", "FASTA-0",
        "FASTQ-0", "FASTQ-Illumina-0", "FASTQ-Int-0", "FASTQ-Solexa-0", "Genbank-0", "PDB-3.2", "Pileup-0",
        "SAI-0.1.2", "SAM-0.1.2", "SBML-1.2", "SBML-2.4.1", "SBML-3.1", "TAB-0", "Text-0", "VCF-3.3", "VCF-4.0",
        "WIG-0", "Unspecified"};

    /**
     * The list of standard multiplicity names.
     */
    private static final String[] MULTIPLICITY_NAMES = {"single", "many", "folder", "collection"};

    /**
     * The list of standard data source names.
     */
    private static final String[] DATA_SOURCE_NAMES = {"file", "stdout", "stderr"};

    /**
     * The list of standard tool type names.
     */
    private static final String[] TOOL_TYPE_NAMES = {"executable", "fAPI" };

    /**
     * Creates a generalized registry to use for testing.
     *
     * @return the registry.
     */
    public static HeterogeneousRegistryImpl createRegistry() {
        HeterogeneousRegistryImpl registry = new HeterogeneousRegistryImpl();
        addTemplates(registry);
        addAnalyses(registry);
        addDeployedComponents(registry);
        return registry;
    }

    /**
     * Adds some deployed components to the generalized registry to use for testing.
     *
     * @param registry the registry.
     */
    private static void addDeployedComponents(HeterogeneousRegistryImpl registry) {
        registry.add(DeployedComponent.class, "foo", createDeployedComponent("foo"));
        registry.add(DeployedComponent.class, "bar", createDeployedComponent("bar"));
    }

    /**
     * Adds some analyses to the generalized registry to use for testing.
     *
     * @param registry the registry.
     */
    private static void addAnalyses(HeterogeneousRegistryImpl registry) {
        registry.add(TransformationActivity.class, "foo", createAnalysis("foo"));
        registry.add(TransformationActivity.class, "bar", createAnalysis("bar"));
    }

    /**
     * Adds some templates to the generalized registry to use for testing.
     *
     * @param registry the registry.
     */
    private static void addTemplates(HeterogeneousRegistryImpl registry) {
        registry.add(Template.class, "foo", createTemplate("foo"));
        registry.add(Template.class, "bar", createTemplate("bar"));
    }

    /**
     * Generates a dummy set of References for a TransformationActivity.
     *
     * @return
     *  References for a transformation activity.
     */
    private static Set<TransformationActivityReference> createTransformationReferences() {
        Set<TransformationActivityReference> results = new HashSet<TransformationActivityReference>();

        TransformationActivityReference ref = new TransformationActivityReference();
        ref.setReferenceText("Nope.");
        results.add(ref);

        ref = new TransformationActivityReference();
        ref.setReferenceText("Chuck Testa");
        results.add(ref);

        return results;
    }

    /**
     * Creates an analysis to use for testing.
     *
     * @param name the name of the analysis.
     * @return the analysis.
     */
    public static TransformationActivity createAnalysis(String name) {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setName(name);
        analysis.setId(name + "id");
        analysis.setJobTypeNames(Arrays.asList("executable"));

        analysis.setIntegrationDatum(createIntegrationDatum());
        analysis.setReferences(createTransformationReferences());

        return analysis;
    }

    /**
     * Creates a template to use for testing.
     *
     * @param name the name of the template.
     * @return the template.
     */
    public static Template createTemplate(String name) {
        return createTemplate(name, name + "component");
    }

    /**
     * Creates a template to use for testing.
     *
     * @param name the name of the template.
     * @param componentId the deployed component identifier.
     * @return the template.
     */
    public static Template createTemplate(String name, String componentId) {
        Template template = new Template();
        template.setName(name);
        template.setId(name + "id");
        template.setComponent(componentId);
        return template;
    }

    /**
     * Creates a deployed component to use for testing.
     *
     * @param name the name of the deployed component.
     * @return the deployed component.
     */
    private static DeployedComponent createDeployedComponent(String name) {
        DeployedComponent component = new DeployedComponent();
        component.setId(name + "id");
        component.setName(name);
        component.setLocation(name + "location");
        component.setToolType(createToolType(name + "type"));
        return component;
    }

    public static IntegrationDatum createIntegrationDatum() {
        IntegrationDatum integrationDatum = new IntegrationDatum();

        integrationDatum.setIntegratorEmail("cookiemonster@iplantc.org");
        integrationDatum.setIntegratorName("Cookie J. Monster");

        return integrationDatum;
    }

    public static Set<DeployedComponentDataFile> createDeployedComponentDataFiles() {
        HashSet<DeployedComponentDataFile> dataFiles = new HashSet<DeployedComponentDataFile>();

        DeployedComponentDataFile inputFile = new DeployedComponentDataFile();
        inputFile.setId(0);
        inputFile.setInputFile(true);
        inputFile.setFilename("foo.in");
        dataFiles.add(inputFile);

        DeployedComponentDataFile outputFile = new DeployedComponentDataFile();
        outputFile.setId(1);
        outputFile.setInputFile(false);
        outputFile.setFilename("bar.out");
        dataFiles.add(outputFile);

        return dataFiles;
    }

    /**
     * Adds the root template group to the a mock template group DAO.
     *
     * @param templateGroupDao the mock template group DAO.
     */
    public static void addRootTemplateGroup(MockTemplateGroupDao templateGroupDao) {
        TemplateGroup root = new TemplateGroup();
        root.setId("root_template_group_id");
        root.setName("template_grouping");
        templateGroupDao.save(root);
    }

    /**
     * Generates a mock property type DAO containing a set of standard property types.
     *
     * @return the DAO.
     */
    public static MockPropertyTypeDao createMockPropertyTypeDao() {
        MockPropertyTypeDao dao = new MockPropertyTypeDao();
        for (String name : PROPERTY_TYPE_NAMES) {
            dao.save(createPropertyType(name));
        }
        return dao;
    }

    /**
     * Creates a property type with the given name.
     *
     * @param name the name of the property type.
     * @return the property type.
     */
    public static PropertyType createPropertyType(String name) {
        PropertyType propertyType = new PropertyType();
        propertyType.setId(ImportUtils.generateId());
        propertyType.setName(name);
        propertyType.setLabel("label of property type " + name);
        propertyType.setDescription("description of property type " + name);
        return propertyType;
    }

    /**
     * Generates a mock rule type DAO containing a set of standard rule types.
     *
     * @return the DAO.
     */
    public static MockRuleTypeDao createMockRuleTypeDao() {
        MockRuleTypeDao dao = new MockRuleTypeDao();
        for (String name : RULE_TYPE_NAMES) {
            dao.save(createRuleType(name));
        }
        return dao;
    }

    /**
     * Creates a rule type with the given name.
     *
     * @param name the name of the rule type.
     * @return the rule type.
     */
    public static RuleType createRuleType(String name) {
        RuleType ruleType = new RuleType();
        ruleType.setId(ImportUtils.generateId());
        ruleType.setName(name);
        ruleType.setLabel("label of rule type " + name);
        ruleType.setDescription("description of rule type " + name);
        return ruleType;
    }

    /**
     * Generates a mock info type DAO containing a set of standard info types.
     *
     * @return the DAO.
     */
    public static MockInfoTypeDao createMockInfoTypeDao() {
        MockInfoTypeDao dao = new MockInfoTypeDao();
        for (String name : INFO_TYPE_NAMES) {
            dao.save(createInfoType(name));
        }
        return dao;
    }

    /**
     * Creates an info type with the given name.
     *
     * @param name the name of the info type.
     * @return the info type.
     */
    public static InfoType createInfoType(String name) {
        InfoType infoType = new InfoType();
        infoType.setId(ImportUtils.generateId());
        infoType.setName(name);
        infoType.setLabel("label of info type " + name);
        infoType.setDescription("description of info type " + name);
        return infoType;
    }

    /**
     * Generates a mock data format DAO containing a set of standard data formats.
     *
     * @return the DAO.
     */
    public static MockDataFormatDao createMockDataFormatDao() {
        MockDataFormatDao dao = new MockDataFormatDao();
        for (String name : DATA_FORMAT_NAMES) {
            dao.save(createDataFormat(name));
        }
        return dao;
    }

    /**
     * Creates a data format with the given name.
     *
     * @param name the data format name.
     * @return the data format.
     */
    public static DataFormat createDataFormat(String name) {
        DataFormat dataFormat = new DataFormat();
        dataFormat.setGuid(ImportUtils.generateId());
        dataFormat.setName(name);
        dataFormat.setLabel("label of info type " + name);

        return dataFormat;
    }

    /**
     * Generates a mock multiplicity DAO containing a set of standard multiplicities.
     *
     * @return the mock multiplicity DAO.
     */
    public static MockMultiplicityDao createMockMultiplicityDao() {
        MockMultiplicityDao dao = new MockMultiplicityDao();
        initializeMultiplicityDao(dao);
        return dao;
    }

    /**
     * Adds a set of standard multiplicities to the given multiplicity DAO.
     *
     * @param dao the multiplicity DAO.
     */
    public static void initializeMultiplicityDao(MockMultiplicityDao dao) {
        for (String name : MULTIPLICITY_NAMES) {
            dao.save(createMultiplicity(name));
        }
    }

    /**
     * Creates a multiplicity with the given name.
     *
     * @param name the multiplicity name.
     * @return the multiplicity.
     */
    public static Multiplicity createMultiplicity(String name) {
        Multiplicity multiplicity = new Multiplicity();
        multiplicity.setId(ImportUtils.generateId());
        multiplicity.setName(name);
        multiplicity.setLabel("label of multiplicity " + name);
        multiplicity.setDescription("description of multiplicity " + name);
        return multiplicity;
    }

    /**
     * Adds the set of standard data sources to a mock data source DAO.
     *
     * @param dao the mock data source DAO.
     */
    public static void initializeDataSourceDao(MockDataSourceDao dao) {
        for (String name : DATA_SOURCE_NAMES) {
            dao.save(createDataSource(name));
        }
    }

    /**
     * Creates a data source with the given name.
     *
     * @param name the data source name.
     * @return the data source.
     */
    public static DataSource createDataSource(String name) {
        DataSource dataSource = new DataSource();
        dataSource.setUuid(ImportUtils.generateId());
        dataSource.setName(name);
        dataSource.setLabel("label of data source " + name);
        dataSource.setDescription("description of data source " + name);
        return dataSource;
    }

    /**
     * Creates a deployed component with the given name and identifier.
     *
     * @param name the deployed component name.
     * @param id the deployed component identifier.
     * @return the deployed component.
     */
    public static DeployedComponent createDeployedComponent(String name, String id) {
        DeployedComponent deployedComponent = new DeployedComponent();
        deployedComponent.setName(name);
        deployedComponent.setId(id);
        deployedComponent.setToolType(createKnownToolType("executable"));
        deployedComponent.setLocation("/path/to/bin");
        deployedComponent.setAttribution(name + "attribution");
        deployedComponent.setVersion(name + "version");
        deployedComponent.setDescription(name + "description");

        deployedComponent.setDeployedComponentDataFiles(createDeployedComponentDataFiles());
        deployedComponent.setIntegrationDatum(createIntegrationDatum());

        return deployedComponent;
    }

    /**
     * Creates a deployed component with the given identifier, name and location.
     *
     * @param id the deployed component identifier.
     * @param name the deployed component name.
     * @param location the deployed component location.
     * @return the deployed component.
     */
    public static DeployedComponent createDeployedComponent(String id, String name, String location) {
        DeployedComponent component = new DeployedComponent();
        component.setId(id);
        component.setName(name);
        component.setLocation(location);
        component.setToolType(createKnownToolType("executable"));
        component.setAttribution(name + "attribution");
        component.setVersion(name + "version");
        component.setDescription(name + "description");
        component.setDeployedComponentDataFiles(createDeployedComponentDataFiles());
        component.setIntegrationDatum(createIntegrationDatum());
        return component;
    }

    /**
     * Creates a tool type with a known name.
     *
     * @param name the name to use.
     * @return the tool type.
     * @throws UnknownToolTypeException if the given name isn't in the list of known tool names.
     */
    public static ToolType createKnownToolType(String name) {
        for (int i = 0; i < TOOL_TYPE_NAMES.length; i++) {
            if (StringUtils.equals(TOOL_TYPE_NAMES[i], name)) {
                return createToolType(i, name);
            }
        }
        throw new UnknownToolTypeException("name", name);
    }

    /**
     * Initializes a tool type DAO for testing.
     *
     * @param dao the DAO to initialize.
     */
    public static void initToolTypeDao(ToolTypeDao dao) {
        for (int i = 0; i < TOOL_TYPE_NAMES.length; i++) {
            dao.save(createToolType(i, TOOL_TYPE_NAMES[i]));
        }
    }

    /**
     * Creates a new tool type with the given name.
     *
     * @param name the tool type name.
     * @return the tool type.
     */
    public static ToolType createToolType(String name) {
        return createToolType(name.hashCode(), name);
    }

    /**
     * Creates a new tool type with the given id and name.
     *
     * @param id the tool type identifier.
     * @param name the tool type name.
     * @return the tool type.
     */
    public static ToolType createToolType(long id, String name) {
        ToolType toolType = new ToolType();
        toolType.setId(id);
        toolType.setName(name);
        toolType.setLabel(name + "-label");
        toolType.setDescription(name + "-description");
        return toolType;
    }

    /**
     * Creates an analysis with one or more transformation steps.
     *
     * @param name the analysis name.
     * @param templateIds the list of template identifiers for the transformation steps.
     * @return the analysis.
     */
    public static TransformationActivity createAnalysisWithSteps(String name, String... templateIds) {
        TransformationActivity analysis = createAnalysis(name);
        for (String templateId : templateIds) {
            analysis.addStep(createTransformationStep(templateId));
        }
        return analysis;
    }

    /**
     * Creates a transformation step for the template with the given identifier.
     *
     * @param templateId the template identifier.
     * @return the transformation step.
     */
    private static TransformationStep createTransformationStep(String templateId) {
        TransformationStep step = new TransformationStep();
        step.setName("transformation_step_for_" + templateId);
        step.setTransformation(createTransformation(templateId));
        return step;
    }

    /**
     * Creates a transformation for the template with the given identifier.
     *
     * @param templateId the template identifier.
     * @return the transformation.
     */
    private static Transformation createTransformation(String templateId) {
        Transformation transformation = new Transformation();
        transformation.setTemplate_id(templateId);
        return transformation;
    }

    /**
     * Creates a workspace for testing.
     *
     * @param username the username.
     * @param rootAnalysisGroupId the root analysis group identifier.
     * @return the workspace.
     */
    public static Workspace createWorkspace(String username, Long rootAnalysisGroupId) {
        Workspace workspace = new Workspace();
        if (username != null) {
            workspace.setUser(createUser(username));
        }
        workspace.setRootAnalysisGroupId(rootAnalysisGroupId);
        return workspace;
    }

    /**
     * Creates a user.
     *
     * @param username the user name.
     * @return the user.
     */
    public static User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    /**
     * Creates a template group.
     *
     * @param name the template group name.
     * @return the template group.
     */
    public static TemplateGroup createTemplateGroup(String name) {
        TemplateGroup templateGroup = new TemplateGroup();
        templateGroup.setId(ImportUtils.generateId());
        templateGroup.setName(name);
        return templateGroup;
    }

    /**
     * Generates a long string.
     *
     * @param len the required string length.
     * @return the string.
     */
    public static String longString(int len) {
        return StringUtils.repeat("a", len);
    }
}
