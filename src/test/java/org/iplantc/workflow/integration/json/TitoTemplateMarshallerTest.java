package org.iplantc.workflow.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.iplantc.persistence.dto.data.DataSource;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.model.PropertyGroup;
import org.iplantc.workflow.model.Rule;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.model.Validator;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.json.
 *
 * @author Dennis Roberts
 */
public class TitoTemplateMarshallerTest {

    /**
     * The marshaler instance being tested.
     */
    private TitoTemplateMarshaller marshaller;

    /**
     * Used to obtain data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * Used register data objects.
     */
    private HeterogeneousRegistryImpl registry;

    /**
     * Initializes each unit test.
     */
    @Before
    public void initialize() {
        registry = new HeterogeneousRegistryImpl();
        initializeDaoFactory();
        marshaller = new TitoTemplateMarshaller(daoFactory, false);
    }

    /**
     * Initializes the DAO factory for testing.
     */
    private void initializeDaoFactory() {
        daoFactory = new MockDaoFactory();
        daoFactory.getDeployedComponentDao().save(UnitTestUtils.createDeployedComponent("component", "componentid"));
        UnitTestUtils.initializeMultiplicityDao(daoFactory.getMockMultiplicityDao());
    }

    /**
     * Verifies that the marshaler correctly marshals the fields in the template.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalTemplateFields() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertEquals("templateid", json.getString("id"));
        assertEquals("templatename", json.getString("name"));
        assertEquals("templatelabel", json.getString("label"));
        assertEquals("component", json.getString("component"));
        assertEquals("componentid", json.getString("component_id"));
        assertEquals("templatetype", json.getString("type"));
    }

    /**
     * Verifies that the marshaler correctly marshals the property group container object required by TITO.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalPropertyGroupContainerFields() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups"));
        JSONObject container = json.getJSONObject("groups");
        assertEquals("--root-PropertyGroupContainer--", container.getString("id"));
        assertEquals("", container.getString("name"));
        assertEquals("", container.getString("label"));
        assertEquals("", container.getString("description"));
        assertTrue(container.getBoolean("isVisible"));
    }

    /**
     * Verifies that the marshaler correctly marshals the property group fields.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalPropertyGroupFields() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups").getJSONArray("groups"));

        JSONArray groups = json.getJSONObject("groups").getJSONArray("groups");
        assertEquals(1, groups.length());

        JSONObject group1 = groups.getJSONObject(0);
        assertEquals("firstpropertygroupid", group1.getString("id"));
        assertEquals("firstpropertygroupname", group1.getString("name"));
        assertEquals("firstpropertygrouplabel", group1.getString("label"));
        assertEquals("firstpropertygroupdescription", group1.getString("description"));
        assertEquals("firstpropertygrouptype", group1.getString("type"));
        assertTrue(group1.getBoolean("isVisible"));
    }

    /**
     * Verifies that the marshaler correctly marshals the property fields.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalPropertyFields() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0));

        JSONObject group1 = json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0);
        assertNotNull(group1.getJSONArray("properties"));

        JSONArray properties = group1.getJSONArray("properties");
        assertEquals(6, properties.length());

        JSONObject property1 = properties.getJSONObject(0);
        assertEquals("firstpropertyid", property1.getString("id"));
        assertEquals("firstpropertyname", property1.getString("name"));
        assertEquals("firstpropertylabel", property1.getString("label"));
        assertEquals("firstpropertydescription", property1.getString("description"));
        assertEquals("firstpropertytypename", property1.getString("type"));
        assertTrue(property1.getBoolean("isVisible"));
        assertEquals("firstpropertyvalue", property1.getString("value"));
        assertTrue(property1.getBoolean("omit_if_blank"));
        assertEquals(27, property1.getInt("order"));

        JSONObject property2 = properties.getJSONObject(1);
        assertEquals("secondpropertyid", property2.getString("id"));
        assertEquals("secondpropertyname", property2.getString("name"));
        assertEquals("secondpropertylabel", property2.getString("label"));
        assertEquals("secondpropertydescription", property2.getString("description"));
        assertEquals("secondpropertytypename", property2.getString("type"));
        assertFalse(property2.getBoolean("isVisible"));
        assertEquals("secondpropertyvalue", property2.getString("value"));
        assertFalse(property2.getBoolean("omit_if_blank"));
        assertEquals(52, property2.getInt("order"));

        JSONObject property3 = properties.getJSONObject(2);
        assertEquals("thirdpropertyid", property3.getString("id"));
        assertEquals("thirdpropertyname", property3.getString("name"));
        assertEquals("thirdpropertylabel", property3.getString("label"));
        assertEquals("thirdpropertydescription", property3.getString("description"));
        assertEquals("thirdpropertytypename", property3.getString("type"));
        assertTrue(property3.getBoolean("isVisible"));
        assertEquals("thirdpropertyvalue", property3.getString("value"));
        assertTrue(property3.getBoolean("omit_if_blank"));
        assertEquals(77, property3.getInt("order"));

        JSONObject property4 = properties.getJSONObject(3);
        assertEquals("fourthpropertyid", property4.getString("id"));
        assertEquals("fourthpropertyname", property4.getString("name"));
        assertEquals("fourthpropertylabel", property4.getString("label"));
        assertEquals("fourthpropertydescription", property4.getString("description"));
        assertEquals("Input", property4.getString("type"));
        assertTrue(property4.getBoolean("isVisible"));
        assertEquals("fourthpropertyvalue", property4.getString("value"));
        assertTrue(property4.getBoolean("omit_if_blank"));
        assertEquals(97, property4.getInt("order"));

        JSONObject property5 = properties.getJSONObject(4);
        assertEquals("fifthpropertyid", property5.getString("id"));
        assertEquals("fifthpropertyname", property5.getString("name"));
        assertEquals("fifthpropertylabel", property5.getString("label"));
        assertEquals("fifthpropertydescription", property5.getString("description"));
        assertEquals("Output", property5.getString("type"));
        assertTrue(property5.getBoolean("isVisible"));
        assertEquals("fifthpropertyvalue", property5.getString("value"));
        assertTrue(property5.getBoolean("omit_if_blank"));
        assertEquals(101, property5.getInt("order"));

        JSONObject property6 = properties.getJSONObject(5);
        assertEquals("sixthpropertyid", property6.getString("id"));
        assertEquals("sixthpropertyname", property6.getString("name"));
        assertEquals("sixthpropertylabel", property6.getString("label"));
        assertEquals("sixthpropertydescription", property6.getString("description"));
        assertEquals("Input", property6.getString("type"));
        assertTrue(property6.getBoolean("isVisible"));
        assertEquals("sixthpropertyvalue", property6.getString("value"));
        assertTrue(property6.getBoolean("omit_if_blank"));
        assertEquals(2112, property6.getInt("order"));
    }

    /**
     * Verifies that the marshaler correctly marshals the validator fields if the validator is present.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalValidatorIfPresent() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0));

        JSONObject group1 = json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0);
        assertNotNull(group1.getJSONArray("properties"));

        JSONArray properties = group1.getJSONArray("properties");
        assertEquals(6, properties.length());

        JSONObject property1 = properties.getJSONObject(0);
        assertFalse(property1.has("validator"));

        JSONObject property2 = properties.getJSONObject(1);
        assertTrue(property2.has("validator"));

        JSONObject validator2 = property2.getJSONObject("validator");
        assertEquals("secondpropertyvalidatorid", validator2.getString("id"));
        assertEquals("secondpropertyvalidatorname", validator2.getString("name"));
        assertTrue(validator2.getBoolean("required"));

        JSONObject property3 = properties.getJSONObject(2);
        assertTrue(property3.has("validator"));

        JSONObject validator3 = property3.getJSONObject("validator");
        assertEquals("thirdpropertyvalidatorid", validator3.getString("id"));
        assertEquals("thirdpropertyvalidatorname", validator3.getString("name"));
        assertFalse(validator3.getBoolean("required"));

        JSONObject property4 = properties.getJSONObject(3);
        assertFalse(property4.has("validator"));

        JSONObject property5 = properties.getJSONObject(4);
        assertFalse(property5.has("validator"));

        JSONObject property6 = properties.getJSONObject(5);
        assertFalse(property6.has("validator"));
    }

    /**
     * Verifies that the marshaller correctly marshals the list of rules if the rules are present.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shoudlMarshalRulesIfPresent() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0));

        JSONObject group1 = json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0);
        assertNotNull(group1.getJSONArray("properties"));

        JSONArray properties = group1.getJSONArray("properties");
        assertEquals(6, properties.length());

        JSONObject property2 = properties.getJSONObject(1);
        assertTrue(property2.has("validator"));

        JSONObject validator2 = property2.getJSONObject("validator");
        assertTrue(validator2.has("rules"));

        JSONObject property3 = properties.getJSONObject(2);
        assertTrue(property3.has("validator"));

        JSONObject validator3 = property3.getJSONObject("validator");
        assertTrue(validator3.has("rules"));

        JSONArray rules2 = validator2.getJSONArray("rules");
        assertEquals(0, rules2.length());

        JSONArray rules3 = validator3.getJSONArray("rules");
        assertEquals(2, rules3.length());

        JSONObject rule1 = rules3.getJSONObject(0);
        assertTrue(rule1.has("firstruletypename"));
        JSONArray rule1args = rule1.getJSONArray("firstruletypename");
        assertEquals(0, rule1args.length());

        JSONObject rule2 = rules3.getJSONObject(1);
        assertTrue(rule2.has("secondruletypename"));
        JSONArray rule2args = rule2.getJSONArray("secondruletypename");
        assertEquals(3, rule2args.length());
        assertEquals("foo", rule2args.getString(0));
        assertEquals("bar", rule2args.getString(1));
        assertEquals("baz", rule2args.getString(2));
    }

    /**
     * Verifies that the marshaller correctly marshals the data object if it's present.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalDataObjectIfPresent() throws JSONException {
        JSONObject json = marshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0));

        JSONObject group1 = json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0);
        assertNotNull(group1.getJSONArray("properties"));

        JSONArray properties = group1.getJSONArray("properties");
        assertEquals(6, properties.length());

        JSONObject property1 = properties.getJSONObject(0);
        assertFalse(property1.has("data_object"));

        JSONObject property2 = properties.getJSONObject(1);
        assertFalse(property2.has("data_object"));

        JSONObject property3 = properties.getJSONObject(2);
        assertFalse(property3.has("data_object"));

        JSONObject property4 = properties.getJSONObject(3);
        assertTrue(property4.has("data_object"));

        JSONObject dataObject4 = property4.getJSONObject("data_object");
        assertEquals("fourthdataobjectid", dataObject4.getString("id"));
        assertEquals("fourthdataobjectname", dataObject4.getString("name"));
        assertEquals("One", dataObject4.getString("multiplicity"));
        assertEquals(97, dataObject4.getInt("order"));
        assertEquals("--foo=", dataObject4.getString("cmdSwitch"));
        assertEquals("File", dataObject4.getString("file_info_type"));
        assertEquals("Unspecified", dataObject4.getString("format"));
        assertEquals("file", dataObject4.getString("data_source"));
        assertEquals("fourthdataobjectdescription", dataObject4.getString("description"));
        assertTrue(dataObject4.getBoolean("required"));
        assertTrue(dataObject4.getBoolean("retain"));

        JSONObject property5 = properties.getJSONObject(4);
        assertTrue(property5.has("data_object"));

        JSONObject dataObject5 = property5.getJSONObject("data_object");
        assertEquals("fifthdataobjectid", dataObject5.getString("id"));
        assertEquals("fifthdataobjectname", dataObject5.getString("output_filename"));
        assertEquals("Many", dataObject5.getString("multiplicity"));
        assertEquals(101, dataObject5.getInt("order"));
        assertEquals("--bar=", dataObject5.getString("cmdSwitch"));
        assertEquals("Tree", dataObject5.getString("file_info_type"));
        assertEquals("NexML", dataObject5.getString("format"));
        assertEquals("stdout", dataObject5.getString("data_source"));
        assertEquals("fifthdataobjectdescription", dataObject5.getString("description"));
        assertFalse(dataObject5.getBoolean("required"));
        assertFalse(dataObject5.getBoolean("retain"));

        JSONObject property6 = properties.getJSONObject(5);
        assertTrue(property6.has("data_object"));

        JSONObject dataObject6 = property6.getJSONObject("data_object");
        assertEquals("sixthdataobjectid", dataObject6.getString("id"));
        assertEquals("sixthdataobjectname", dataObject6.getString("name"));
        assertEquals("Folder", dataObject6.getString("multiplicity"));
        assertEquals(2112, dataObject6.getInt("order"));
        assertEquals("--baz=", dataObject6.getString("cmdSwitch"));
        assertEquals("TraitFile", dataObject6.getString("file_info_type"));
        assertEquals("CSV", dataObject6.getString("format"));
        assertEquals("file", dataObject6.getString("data_source"));
        assertEquals("sixthdataobjectdescription", dataObject6.getString("description"));
        assertFalse(dataObject6.getBoolean("required"));
        assertTrue(dataObject6.getBoolean("retain"));
    }

    /**
     * Verifies that the marshaller uses backward references when we tell it to.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldUseBackwardReferences() throws JSONException {
        marshaller = new TitoTemplateMarshaller(daoFactory, true);
        JSONObject template = marshaller.toJson(createBasicTemplate());
        assertFalse(template.has("component_id"));
        assertEquals("component", template.getString("component_ref"));
    }

    /**
     * Verifies that failure to find a component does not cause an exception.
     */
    @Test()
    public void shouldThrowExceptionIfComponentNotFound() {
        Template template = createBasicTemplate();
        template.setComponent("unknown");
        new TitoTemplateMarshaller(daoFactory, true).toJson(template);
    }

    /**
     * Verifies that the marshaller generates properties for old-style inputs.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldGeneratePropertiesForOldStyleInputs() throws JSONException {
        JSONObject template = marshaller.toJson(createOldStyleTemplate());

        JSONArray groups = template.getJSONObject("groups").getJSONArray("groups");
        assertEquals(3, groups.length());

        JSONObject group1 = groups.getJSONObject(0);
        assertTrue(group1.getString("id").matches("[0-9A-F]{8}(?:-[0-9A-F]{4}){3}-[0-9A-F]{12}"));
        assertEquals("Select data:", group1.getString("name"));
        assertEquals("Select input data", group1.getString("label"));
        assertEquals("", group1.getString("description"));
        assertEquals("step", group1.getString("type"));
        assertTrue(group1.getBoolean("isVisible"));
        assertTrue(group1.has("properties"));

        JSONArray properties = group1.getJSONArray("properties");
        assertEquals(1, properties.length());

        JSONObject prop1 = properties.getJSONObject(0);
        assertTrue(prop1.getString("id").matches("[0-9A-F]{8}(?:-[0-9A-F]{4}){3}-[0-9A-F]{12}"));
        assertEquals("--input1=", prop1.getString("name"));
        assertEquals("input1label", prop1.getString("label"));
        assertEquals("input1description", prop1.getString("description"));
        assertEquals("Input", prop1.getString("type"));
        assertTrue(prop1.getBoolean("isVisible"));
        assertEquals("", prop1.getString("value"));
        assertTrue(prop1.getBoolean("omit_if_blank"));
        assertEquals(11, prop1.getInt("order"));
        assertTrue(prop1.has("data_object"));

        JSONObject dataObject1 = prop1.getJSONObject("data_object");
        assertEquals("input1id", dataObject1.getString("id"));
        assertEquals("input1", dataObject1.getString("name"));
        assertEquals("One", dataObject1.getString("multiplicity"));
        assertEquals(11, dataObject1.getInt("order"));
        assertEquals("--input1=", dataObject1.getString("cmdSwitch"));
        assertEquals("input1infotype", dataObject1.getString("file_info_type"));
        assertEquals("input1dataformat", dataObject1.getString("format"));
        assertEquals("input1description", dataObject1.getString("description"));
        assertTrue(dataObject1.getBoolean("required"));
        assertTrue(dataObject1.getBoolean("retain"));
    }

    /**
     * Verifies that the marshaller generates properties for new-style inputs.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldGeneratePropertiesForNewStyleInputs() throws JSONException {
        JSONObject template = marshaller.toJson(createOldStyleTemplate());

        JSONArray groups = template.getJSONObject("groups").getJSONArray("groups");
        assertEquals(3, groups.length());

        JSONObject group3 = groups.getJSONObject(2);
        assertTrue(group3.getString("id").matches("[0-9A-F]{8}(?:-[0-9A-F]{4}){3}-[0-9A-F]{12}"));
        assertEquals("Output files", group3.getString("name"));
        assertEquals("Output files", group3.getString("label"));
        assertEquals("", group3.getString("description"));
        assertEquals("step", group3.getString("type"));
        assertFalse(group3.getBoolean("isVisible"));
        assertTrue(group3.has("properties"));

        JSONArray properties = group3.getJSONArray("properties");
        assertEquals(1, properties.length());

        JSONObject prop1 = properties.getJSONObject(0);
        assertTrue(prop1.getString("id").matches("[0-9A-F]{8}(?:-[0-9A-F]{4}){3}-[0-9A-F]{12}"));
        assertEquals("--output1=", prop1.getString("name"));
        assertEquals("output1label", prop1.getString("label"));
        assertEquals("output1description", prop1.getString("description"));
        assertEquals("Output", prop1.getString("type"));
        assertFalse(prop1.getBoolean("isVisible"));
        assertEquals("", prop1.getString("value"));
        assertTrue(prop1.getBoolean("omit_if_blank"));
        assertEquals(12, prop1.getInt("order"));
        assertTrue(prop1.has("data_object"));

        JSONObject dataObject1 = prop1.getJSONObject("data_object");
        assertEquals("output1id", dataObject1.getString("id"));
        assertEquals("output1", dataObject1.getString("output_filename"));
        assertEquals("One", dataObject1.getString("multiplicity"));
        assertEquals(12, dataObject1.getInt("order"));
        assertEquals("--output1=", dataObject1.getString("cmdSwitch"));
        assertEquals("output1infotype", dataObject1.getString("file_info_type"));
        assertEquals("output1dataformat", dataObject1.getString("format"));
        assertEquals("output1description", dataObject1.getString("description"));
        assertFalse(dataObject1.getBoolean("required"));
        assertFalse(dataObject1.getBoolean("retain"));
    }

    /**
     * Verifies that identifiers are not retained if the NoIdRetentionStrategy is used.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldNotRetainIdentifiersWithNoIdRetentionStrategy() throws JSONException {
        TitoTemplateMarshaller localMarshaller = new TitoTemplateMarshaller(daoFactory, true,
                new NoIdRetentionStrategy());
        JSONObject json = localMarshaller.toJson(createBasicTemplate());
        assertNotNull(json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0));

        assertFalse("templateid".equals(json.getString("id")));

        JSONObject group1 = json.getJSONObject("groups").getJSONArray("groups").getJSONObject(0);
        assertFalse("firstpropertygroupid".equals(group1.getString("id")));
        assertNotNull(group1.getJSONArray("properties"));

        JSONArray properties = group1.getJSONArray("properties");

        JSONObject property1 = properties.getJSONObject(0);
        assertFalse("firstpropertyid".equals(property1.getString("id")));

        JSONObject property2 = properties.getJSONObject(1);
        assertFalse("secondpropertyid".equals(property2.getString("id")));

        JSONObject property3 = properties.getJSONObject(2);
        assertFalse("thirdpropertyid".equals(property3.getString("id")));

        JSONObject property4 = properties.getJSONObject(3);
        assertFalse("fourthpropertyid".equals(property4.getString("id")));
        assertTrue(property4.has("data_object"));

        JSONObject dataObject4 = property4.getJSONObject("data_object");
        assertFalse("fourthdataobjectid".equals(dataObject4.getString("id")));

        JSONObject property5 = properties.getJSONObject(4);
        assertFalse("fifthpropertyid".equals(property5.getString("id")));
        assertTrue(property5.has("data_object"));

        JSONObject dataObject5 = property5.getJSONObject("data_object");
        assertFalse("fifthdataobjectid".equals(dataObject5.getString("id")));

        JSONObject property6 = properties.getJSONObject(5);
        assertFalse("sixthpropertyid".equals(property6.getString("id")));
        assertTrue(property6.has("data_object"));

        JSONObject dataObject6 = property6.getJSONObject("data_object");
        assertFalse("sixthdataobjectid".equals(dataObject6.getString("id")));
    }

    /**
     * Creates an old-style template for testing.
     *
     * @return the template.
     */
    private Template createOldStyleTemplate() {
        Template template = createBaseTemplate();
        template.setInputs(createOldStyleInputs());
        template.setPropertyGroups(createBasicPropertyGroups());
        template.setOutputs(createOldStyleOutputs());
        return template;
    }

    /**
     * Creates a list of old-style outputs.
     *
     * @return the list of outputs.
     */
    private List<DataObject> createOldStyleOutputs() {
        List<DataObject> outputs = new ArrayList<DataObject>();
        outputs.add(createDataObject("output1", "single", 12, false, false));
        return outputs;
    }

    /**
     * Creates a list of old-style inputs.
     *
     * @return the list of inputs.
     */
    private List<DataObject> createOldStyleInputs() {
        List<DataObject> inputs = new ArrayList<DataObject>();
        inputs.add(createDataObject("input1", "single", 11, true, true));
        inputs.add(getFourthPropertyDataObject());
        return inputs;
    }

    /**
     * Creates a data object for testing.
     *
     * @param name the data object name.
     * @param multiplicity the multiplicity name.
     * @param order the order indicator.
     * @param required true if the data object is required.
     * @param retain true if the data object should be retained after the analysis has been executed.
     * @return the data object.
     */
    private DataObject createDataObject(String name, String multiplicity, int order, boolean required, boolean retain) {
        DataObject dataObject = new DataObject();
        dataObject.setId(name + "id");
        dataObject.setName(name);
        dataObject.setLabel(name + "label");
        dataObject.setMultiplicity(daoFactory.getMultiplicityDao().findUniqueInstanceByName(multiplicity));
        dataObject.setOrderd(order);
        dataObject.setSwitchString("--" + name + "=");
        dataObject.setInfoType(UnitTestUtils.createInfoType(name + "infotype"));
        dataObject.setDataFormat(UnitTestUtils.createDataFormat(name + "dataformat"));
        dataObject.setDescription(name + "description");
        dataObject.setRequired(required);
        dataObject.setRetain(retain);
        return dataObject;
    }

    /**
     * Creates a basic template to use for testing.
     *
     * @return the template.
     */
    private Template createBasicTemplate() {
        Template template = createBaseTemplate();
        template.setPropertyGroups(createBasicPropertyGroups());
        return template;
    }

    /**
     * Creates a base template to use for testing.
     *
     * @return the base template.
     */
    private Template createBaseTemplate() {
        Template template = new Template();
        template.setId("templateid");
        template.setName("templatename");
        template.setLabel("templatelabel");
        template.setComponent("componentid");
        template.setType("templatetype");
        return template;
    }

    /**
     * Creates a basic list of property groups to use for testing.
     *
     * @return the list of property groups.
     */
    private List<PropertyGroup> createBasicPropertyGroups() {
        List<PropertyGroup> groups = new ArrayList<PropertyGroup>();
        groups.add(createFirstBasicPropertyGroup());
        return groups;
    }

    /**
     * Creates the first property group in the basic list of property groups to use for testing.
     *
     * @return the property group.
     */
    private PropertyGroup createFirstBasicPropertyGroup() {
        PropertyGroup group = new PropertyGroup();
        group.setId("firstpropertygroupid");
        group.setName("firstpropertygroupname");
        group.setLabel("firstpropertygrouplabel");
        group.setDescription("firstpropertygroupdescription");
        group.setGroupType("firstpropertygrouptype");
        group.setVisible(true);
        group.setProperties(createFirstBasicPropertyList());
        return group;
    }

    /**
     * Creates the first basic property in the first basic property group.
     *
     * @return the property list.
     */
    private List<Property> createFirstBasicPropertyList() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(createFirstBasicProperty());
        properties.add(createSecondBasicProperty());
        properties.add(createThirdBasicProperty());
        properties.add(createFourthBasicProperty());
        properties.add(createFifthBasicProperty());
        properties.add(createSixthBasicProperty());
        return properties;
    }

    /**
     * Creates the first basic property in the first basic property group.
     *
     * @return the property.
     */
    private Property createFirstBasicProperty() {
        Property property = new Property();
        property.setId("firstpropertyid");
        property.setName("firstpropertyname");
        property.setLabel("firstpropertylabel");
        property.setDescription("firstpropertydescription");
        property.setPropertyType(UnitTestUtils.createPropertyType("firstpropertytypename"));
        property.setVisible(true);
        property.setDefaultValue("firstpropertyvalue");
        property.setOmitIfBlank(true);
        property.setOrder(27);
        return property;
    }

    /**
     * Creates the second basic property in the first basic property group.
     *
     * @return the property.
     */
    private Property createSecondBasicProperty() {
        Property property = new Property();
        property.setId("secondpropertyid");
        property.setName("secondpropertyname");
        property.setLabel("secondpropertylabel");
        property.setDescription("secondpropertydescription");
        property.setPropertyType(UnitTestUtils.createPropertyType("secondpropertytypename"));
        property.setVisible(false);
        property.setDefaultValue("secondpropertyvalue");
        property.setOmitIfBlank(false);
        property.setOrder(52);
        property.setValidator(createSecondBasicPropertyValidator());
        return property;
    }

    /**
     * Creates the validator for the second basic property in the first basic property group.
     *
     * @return the property.
     */
    private Validator createSecondBasicPropertyValidator() {
        Validator validator = new Validator();
        validator.setId("secondpropertyvalidatorid");
        validator.setName("secondpropertyvalidatorname");
        validator.setRequired(true);
        return validator;
    }

    /**
     * Creates the third basic property in the first basic property group.
     *
     * @return the property.
     */
    private Property createThirdBasicProperty() {
        Property property = new Property();
        property.setId("thirdpropertyid");
        property.setName("thirdpropertyname");
        property.setLabel("thirdpropertylabel");
        property.setDescription("thirdpropertydescription");
        property.setPropertyType(UnitTestUtils.createPropertyType("thirdpropertytypename"));
        property.setVisible(true);
        property.setDefaultValue("thirdpropertyvalue");
        property.setOmitIfBlank(true);
        property.setOrder(77);
        property.setValidator(createThirdBasicPropertyValidator());
        return property;
    }

    /**
     * Creates the validator for the third basic property in the first basic property group.
     *
     * @return the validator.
     */
    private Validator createThirdBasicPropertyValidator() {
        Validator validator = new Validator();
        validator.setId("thirdpropertyvalidatorid");
        validator.setName("thirdpropertyvalidatorname");
        validator.setRequired(false);
        validator.setRules(createThirdValidatorRuleList());
        return validator;
    }

    /**
     * Creates the list of rules for the third property validator.
     *
     * @return the list of rules.
     */
    private List<Rule> createThirdValidatorRuleList() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(createFirstRule());
        rules.add(createSecondRule());
        return rules;
    }

    /**
     * Creates the first rule for the third property validator.
     *
     * @return the rule.
     */
    private Rule createFirstRule() {
        Rule rule = new Rule();
        rule.setId("firstruleid");
        rule.setName("firstrulename");
        rule.setLabel("firstrulelabel");
        rule.setDescription("firstruledescription");
        rule.setRuleType(UnitTestUtils.createRuleType("firstruletypename"));
        return rule;
    }

    /**
     * Creates the second rule for the third property validator.
     *
     * @return the rule.
     */
    private Rule createSecondRule() {
        Rule rule = new Rule();
        rule.setId("secondruleid");
        rule.setName("secondrulename");
        rule.setLabel("secondrulelabel");
        rule.setDescription("secondruledescription");
        rule.setRuleType(UnitTestUtils.createRuleType("secondruletypename"));
        rule.setArguments(Arrays.asList("foo", "bar", "baz"));
        return rule;
    }

    /**
     * Creates the fourth basic property.
     *
     * @return the property.
     */
    private Property createFourthBasicProperty() {
        Property property = new Property();
        property.setId("fourthpropertyid");
        property.setName("fourthpropertyname");
        property.setLabel("fourthpropertylabel");
        property.setDescription("fourthpropertydescription");
        property.setPropertyType(UnitTestUtils.createPropertyType("Input"));
        property.setVisible(true);
        property.setDefaultValue("fourthpropertyvalue");
        property.setOmitIfBlank(true);
        property.setOrder(97);
        property.setDataObject(getFourthPropertyDataObject());
        return property;
    }

    /**
     * Gets the data object for the fourth basic property without producing a duplicate.
     *
     * @return the data object.
     */
    private DataObject getFourthPropertyDataObject() {
        DataObject dataObject = registry.get(DataObject.class, "fourthdataobjectname");
        if (dataObject == null) {
            dataObject = createFourthPropertyDataObject();
            registry.add(DataObject.class, dataObject.getName(), dataObject);
        }
        return dataObject;
    }

    /**
     * Creates the data object for the fourth basic property.
     *
     * @return the data object.
     */
    private DataObject createFourthPropertyDataObject() {
        DataObject dataObject = new DataObject();
        dataObject.setId("fourthdataobjectid");
        dataObject.setName("fourthdataobjectname");
        dataObject.setMultiplicity(UnitTestUtils.createMultiplicity("single"));
        dataObject.setOrderd(97);
        dataObject.setSwitchString("--foo=");
        dataObject.setInfoType(UnitTestUtils.createInfoType("File"));
        dataObject.setDataFormat(UnitTestUtils.createDataFormat("Unspecified"));
        dataObject.setDataSource(getFileDataSource());
        dataObject.setDescription("fourthdataobjectdescription");
        dataObject.setRequired(true);
        dataObject.setRetain(true);
        return dataObject;
    }

    /**
     * Creates the fifth basic property.
     *
     * @return the property.
     */
    private Property createFifthBasicProperty() {
        Property property = new Property();
        property.setId("fifthpropertyid");
        property.setName("fifthpropertyname");
        property.setLabel("fifthpropertylabel");
        property.setDescription("fifthpropertydescription");
        property.setPropertyType(UnitTestUtils.createPropertyType("Output"));
        property.setVisible(true);
        property.setDefaultValue("fifthpropertyvalue");
        property.setOmitIfBlank(true);
        property.setOrder(101);
        property.setDataObject(registerDataObject(createFifthPropertyDataObject()));
        return property;
    }

    /**
     * Creates the data object for the fifth basic property.
     *
     * @return the data object.
     */
    private DataObject createFifthPropertyDataObject() {
        DataObject dataObject = new DataObject();
        dataObject.setId("fifthdataobjectid");
        dataObject.setName("fifthdataobjectname");
        dataObject.setMultiplicity(UnitTestUtils.createMultiplicity("many"));
        dataObject.setOrderd(101);
        dataObject.setSwitchString("--bar=");
        dataObject.setInfoType(UnitTestUtils.createInfoType("Tree"));
        dataObject.setDataFormat(UnitTestUtils.createDataFormat("NexML"));
        dataObject.setDataSource(getStdoutDataSource());
        dataObject.setDescription("fifthdataobjectdescription");
        dataObject.setRequired(false);
        dataObject.setRetain(false);
        return dataObject;
    }

    /**
     * Creates the sixth basic property.
     *
     * @return the property.
     */
    private Property createSixthBasicProperty() {
        Property property = new Property();
        property.setId("sixthpropertyid");
        property.setName("sixthpropertyname");
        property.setLabel("sixthpropertylabel");
        property.setDescription("sixthpropertydescription");
        property.setPropertyType(UnitTestUtils.createPropertyType("Input"));
        property.setVisible(true);
        property.setDefaultValue("sixthpropertyvalue");
        property.setOmitIfBlank(true);
        property.setOrder(2112);
        property.setDataObject(registerDataObject(createSixthPropertyDataObject()));
        return property;
    }

    /**
     * Creates the data object for the sixth basic property.
     *
     * @return the data object.
     */
    private DataObject createSixthPropertyDataObject() {
        DataObject dataObject = new DataObject();
        dataObject.setId("sixthdataobjectid");
        dataObject.setName("sixthdataobjectname");
        dataObject.setMultiplicity(UnitTestUtils.createMultiplicity("collection"));
        dataObject.setOrderd(2112);
        dataObject.setSwitchString("--baz=");
        dataObject.setInfoType(UnitTestUtils.createInfoType("TraitFile"));
        dataObject.setDataFormat(UnitTestUtils.createDataFormat("CSV"));
        dataObject.setDataSource(getFileDataSource());
        dataObject.setDescription("sixthdataobjectdescription");
        dataObject.setRequired(false);
        dataObject.setRetain(true);
        return dataObject;
    }

    /**
     * Registers a data object so that we can refer to it later.
     *
     * @param dataObject the data object to register.
     * @return the data object.
     */
    private DataObject registerDataObject(DataObject dataObject) {
        registry.add(DataObject.class, dataObject.getName(), dataObject);
        return dataObject;
    }

    /**
     * @return the data source to use for files.
     */
    private DataSource getFileDataSource() {
        DataSource dataSource = registry.get(DataSource.class, "file");
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setId(1);
            dataSource.setUuid("8D6B8247-F1E7-49DB-9FFE-13EAD7C1AED6");
            dataSource.setName("file");
            dataSource.setLabel("File");
            dataSource.setDescription("A regular file.");
            registry.add(DataSource.class, dataSource.getName(), dataSource);
        }
        return dataSource;
    }

    /**
     * @return the data source to use for standard output.
     */
    private DataSource getStdoutDataSource() {
        DataSource dataSource = registry.get(DataSource.class, "stdout");
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setId(2);
            dataSource.setUuid("1EEECF26-367A-4038-8D19-93EA80741DF2");
            dataSource.setName("stdout");
            dataSource.setLabel("Standard Output");
            dataSource.setDescription("Redirected standard output from a job.");
            registry.add(DataSource.class, dataSource.getName(), dataSource);
        }
        return dataSource;
    }
}
