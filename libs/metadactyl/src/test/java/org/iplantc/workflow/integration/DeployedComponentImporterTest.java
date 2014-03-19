package org.iplantc.workflow.integration;

import java.io.IOException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.iplantc.workflow.util.JsonTestDataImporter.getTestJSONObject;

import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.UnknownToolTypeException;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.DeployedComponentImporter.
 *
 * @author Dennis Roberts
 */
public class DeployedComponentImporterTest {

    /**
     * Used to obtain data access objects.
     */
    private MockDaoFactory daoFactory = new MockDaoFactory();

    /**
     * The deployed component importer instance being tested.
     */
    private DeployedComponentImporter deployedComponentImporter;

    /**
     * Initializes each unit test.
     */
    @Before
    public void initialize() {
		daoFactory = new MockDaoFactory();
        UnitTestUtils.initToolTypeDao(daoFactory.getToolTypeDao());
        deployedComponentImporter = new DeployedComponentImporter(daoFactory);
    }

    /**
     * Verifies that we can import a fully specified component.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void testFullySpecifiedComponent() throws JSONException {
        JSONObject json = generateJson("someid", "foo", "bar", "executable", "blarg", "glarb", "quux");
        deployedComponentImporter.importObject(json);
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        DeployedComponent component = daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0);
        assertEquals("someid", component.getId());
        assertEquals("foo", component.getName());
        assertEquals("bar", component.getLocation());
        assertEquals("executable", component.getType());
        assertEquals("blarg", component.getDescription());
        assertEquals("glarb", component.getVersion());
        assertEquals("quux", component.getAttribution());
    }

    /**
     * Verifies that we can import a minimally specified component.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void testMinimallySpecifiedComponent() throws JSONException {
        JSONObject json = generateJson(null, "name", "location", "executable", null, null, null);
        deployedComponentImporter.importObject(json);
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        DeployedComponent component = daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0);
        assertTrue(component.getId().matches("[-0-9A-F]{36}"));
        assertEquals("name", component.getName());
        assertEquals("location", component.getLocation());
        assertEquals("executable", component.getType());
        assertNull(component.getDescription());
        assertNull(component.getVersion());
        assertNull(component.getAttribution());
    }

    /**
     * Verifies that the importer will match an existing component on name and location.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldMatchExistingComponentsOnNameAndLocation() throws JSONException {
        deployedComponentImporter.enableReplacement();
        JSONObject original = generateJson(null, "name", "location", "executable", null, null, null);
        deployedComponentImporter.importObject(original);
        JSONObject updated = generateJson(null, "name", "location", "fAPI", null, null, null);
        deployedComponentImporter.importObject(updated);
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        DeployedComponent component = daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0);
        assertEquals("name", component.getName());
        assertEquals("location", component.getLocation());
        assertEquals("fAPI", component.getType());
    }

    /**
     * Verifies that the importer will not match an existing component on name alone.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldNotMatchExistingComponentOnNameAlone() throws JSONException {
        deployedComponentImporter.enableReplacement();
        JSONObject original = generateJson(null, "name", "location", "executable", null, null, null);
        deployedComponentImporter.importObject(original);
        JSONObject updated = generateJson(null, "name", "noitacol", "fAPI", null, null, null);
        deployedComponentImporter.importObject(updated);
        assertEquals(2, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
    }

    /**
     * Verifies that the importer will not match an existing component on location alone.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldNotMatchExistingComponentOnLocationAlone() throws JSONException {
        deployedComponentImporter.enableReplacement();
        JSONObject original = generateJson(null, "name", "location", "executable", null, null, null);
        deployedComponentImporter.importObject(original);
        JSONObject updated = generateJson(null, "eman", "location", "fAPI", null, null, null);
        deployedComponentImporter.importObject(updated);
        assertEquals(2, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
    }

    /**
     * Verifies that the importer will always match an existing component by identifier, even if the name, location
     * or both change.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldAlwaysMatchExistingComponentOnId() throws JSONException {
        deployedComponentImporter.enableReplacement();
        deployedComponentImporter.importObject(generateJson("i", "n", "l", "executable", "d", "v", "a"));
        deployedComponentImporter.importObject(generateJson("i", "o", "l", "executable", "d", "v", "a"));
        deployedComponentImporter.importObject(generateJson("i", "n", "m", "executable", "d", "v", "a"));
        deployedComponentImporter.importObject(generateJson("i", "o", "m", "executable", "d", "v", "a"));
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
    }

    /**
     * Verifies that a missing name generates an exception.
     *
     * @throws JSONException if the JSON we try to submit is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingNameShouldCauseException() throws JSONException {
        JSONObject json = generateJson(null, null, "location", "executable", null, null, null);
        deployedComponentImporter.importObject(json);
    }

    /**
     * Verifies that a missing location generates an exception.
     *
     * @throws JSONException if the JSON we try to submit is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingLocationShouldCauseException() throws JSONException {
        JSONObject json = generateJson(null, "name", null, "executable", null, null, null);
        deployedComponentImporter.importObject(json);
    }

    /**
     * Verifies that a missing type generates an exception.
     *
     * @throws JSONException if the JSON we try to submit is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingTypeShouldCauseException() throws JSONException {
        JSONObject json = generateJson(null, "name", "location", null, null, null, null);
        deployedComponentImporter.importObject(json);
    }

    /**
     * Verifies that an unknown deployed component type generates an exception.
     *
     * @throws UnknownToolTypeException if the tool type isn't found (expected result).
     * @throws JSONException if a JSON error occurs.
     */
    @Test(expected = UnknownToolTypeException.class)
    public void unknownTypeShouldCauseException() throws UnknownToolTypeException, JSONException {
        JSONObject json = generateJson(null, "name", "location", "blah", null, null, null);
        deployedComponentImporter.importObject(json);
    }

    /**
     * Verifies that we can import multiple components at once.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void testMultipleComponents() throws JSONException {
        JSONArray array = new JSONArray();
        array.put(generateJson("someid", "foo", "bar", "executable", "blarg", "glarb", "quux"));
        array.put(generateJson(null, "name", "location", "fAPI", null, null, null));
        deployedComponentImporter.importObjectList(array);
        assertEquals(2, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        DeployedComponent component1 = daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0);
        DeployedComponent component2 = daoFactory.getMockDeployedComponentDao().getSavedObjects().get(1);
        assertEquals("someid", component1.getId());
        assertEquals("foo", component1.getName());
        assertEquals("bar", component1.getLocation());
        assertEquals("executable", component1.getType());
        assertEquals("blarg", component1.getDescription());
        assertEquals("glarb", component1.getVersion());
        assertEquals("quux", component1.getAttribution());
        assertTrue(component2.getId().matches("[-0-9A-F]{36}"));
        assertEquals("name", component2.getName());
        assertEquals("location", component2.getLocation());
        assertEquals("fAPI", component2.getType());
        assertNull(component2.getDescription());
        assertNull(component2.getVersion());
        assertNull(component2.getAttribution());
    }

    /**
     * Verifies that the importer will register deployed components if a registry is specified.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldRegisterDeployedComponents() throws JSONException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        deployedComponentImporter.setRegistry(registry);
        deployedComponentImporter.importObject(generateJson(null, "roo", "rar", "executable", null, null, null));
        assertEquals(3, registry.size(DeployedComponent.class));
        assertNotNull(registry.get(DeployedComponent.class, "roo"));
    }

    /**
     * Verifies that the importer will register deployed components when a list of deployed components is being created.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldRegisterMultipleDeployedComponents() throws JSONException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        deployedComponentImporter.setRegistry(registry);
        JSONArray array = new JSONArray();
        array.put(generateJson(null, "roo", "rar", "executable", null, null, null));
        array.put(generateJson(null, "glarb", "blrfl", "fAPI", null, null, null));
        deployedComponentImporter.importObjectList(array);
        assertEquals(4, registry.size(DeployedComponent.class));
        assertNotNull(registry.get(DeployedComponent.class, "roo"));
        assertNotNull(registry.get(DeployedComponent.class, "glarb"));
    }

    /**
     * Verifies that the importer silently ignore attempts to replace an existing deployed component if replacement is
     * disabled.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldNotReplaceExistingDeployedComponentIfReplacementDisabled() throws JSONException {
        JSONArray array = new JSONArray();
        array.put(generateJson(null, "zaz", "/usr/bin", "executable", null, null, null));
        array.put(generateJson(null, "zaz", "/usr/bin", "fAPI", null, null, null));
        deployedComponentImporter.importObjectList(array);
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        assertEquals("executable", daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0).getType());
    }

    /**
     * Verifies that the importer still registers a duplicate deployed component in the registry, even if no update is
     * performed.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldRegisterExistingDeployedComponentIfReplacementDisabled() throws JSONException {
        deployedComponentImporter.importObject(generateJson(null, "zaz", "/usr/bin", "executable", null, null, null));
        deployedComponentImporter.disableReplacement();
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        deployedComponentImporter.setRegistry(registry);
        deployedComponentImporter.importObject(generateJson(null, "zaz", "/usr/bin", "fAPI", null, null, null));
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        assertEquals("executable", daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0).getType());
        assertNotNull(registry.get(DeployedComponent.class, "zaz"));
    }

    /**
     * Verifies that the importer silently ignore attempts to replace an existing deployed component if replacement is
     * disabled.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldNotReplaceExistingDeployedComponentIfReplacementIgnored() throws JSONException {
        JSONArray array = new JSONArray();
        array.put(generateJson(null, "zaz", "/usr/bin", "executable", null, null, null));
        array.put(generateJson(null, "zaz", "/usr/bin", "fAPI", null, null, null));
        deployedComponentImporter.ignoreReplacement();
        deployedComponentImporter.importObjectList(array);
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        assertEquals("executable", daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0).getType());
    }

    /**
     * Verifies that the importer still registers a duplicate deployed component in the registry, even if no update is
     * performed.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldRegisterExistingDeployedComponentIfReplacementIgnored() throws JSONException {
        deployedComponentImporter.importObject(generateJson(null, "zaz", "/usr/bin", "executable", null, null, null));
        deployedComponentImporter.ignoreReplacement();
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        deployedComponentImporter.setRegistry(registry);
        deployedComponentImporter.importObject(generateJson(null, "zaz", "/usr/bin", "fAPI", null, null, null));
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        assertEquals("executable", daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0).getType());
        assertNotNull(registry.get(DeployedComponent.class, "zaz"));
    }

    /**
     * Verifies that the importer will replace an existing deployed component if it's configured to do so.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldReplaceExistingDeployedComponentIfReplacementEnabled() throws JSONException {
        JSONArray array = new JSONArray();
        array.put(generateJson(null, "bar", "/usr/bin", "executable", null, null, null));
        array.put(generateJson(null, "bar", "/usr/bin", "fAPI", null, null, null));
        deployedComponentImporter.enableReplacement();
        deployedComponentImporter.importObjectList(array);
        assertEquals(1, daoFactory.getMockDeployedComponentDao().getSavedObjects().size());
        assertEquals("fAPI", daoFactory.getMockDeployedComponentDao().getSavedObjects().get(0).getType());
    }

    /**
     * Verifies that the importer will add an existing deployed component to the registry if it's updated.
     *
     * @throws JSONException if we try to use an invalid attribute name.
     */
    @Test
    public void shouldAddUpdatedExistingDeployedComponentToRegistry() throws JSONException {
        deployedComponentImporter.enableReplacement();
        deployedComponentImporter.importObject(generateJson("foo", "bar", "/usr/bin", "executable", null, null, null));
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        deployedComponentImporter.setRegistry(registry);
        deployedComponentImporter.importObject(generateJson("foo", "rab", "/usr/bin", "fAPI", null, null, null));
        assertNotNull(registry.get(DeployedComponent.class, "rab"));
        assertEquals("foo", registry.get(DeployedComponent.class, "rab").getId());
        assertEquals("fAPI", registry.get(DeployedComponent.class, "rab").getType());
    }

    /**
     * Generates the JSON object to pass to the import service.
     *
     * @param id the component identifier.
     * @param name the component name.
     * @param location the component location.
     * @param type the component type.
     * @param description the component description.
     * @param version the component version.
     * @param attribution the component attribution.
     * @return the JSON object.
     * @throws JSONException if we try to use an invalid attribute name.
     */
    private JSONObject generateJson(String id, String name, String location, String type, String description,
            String version, String attribution) throws JSONException {
            JSONObject json = new JSONObject();
        putIfNotNull(json, "id", id);
        json.put("name", name);
        putIfNotNull(json, "location", location);
        json.put("type", type);
        putIfNotNull(json, "description", description);
        putIfNotNull(json, "version", version);
        putIfNotNull(json, "attribution", attribution);

        try {
            json.put("implementation", getTestJSONObject("implementation_fragment"));
        }
        catch (IOException ioException) {
            throw new RuntimeException("Unable to load json", ioException);
        }

        return json;
    }

    /**
     * Adds a value to a JSON object if the value is not null.
     *
     * @param json the JSON object.
     * @param name the name of the value.
     * @param value the actual value.
     * @throws JSONException if the name is invalid.
     */
    private void putIfNotNull(JSONObject json, String name, String value) throws JSONException {
        if (value != null) {
            json.put(name, value);
        }
    }
}
