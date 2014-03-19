package org.iplantc.workflow.integration;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.dao.mock.MockDeployedComponentDao;
import org.junit.Before;
import org.junit.Test;

import static org.iplantc.workflow.util.UnitTestUtils.createDeployedComponent;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link DeployedComponentExporter}.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentExporterTest {
    
    /**
     * The DAO factory to use for testing.
     */
    private MockDaoFactory mockDaoFactory;

    /**
     * The deployed component exporter instance being tested.
     */
    private DeployedComponentExporter exporter;

    /**
     * Initializes each unit test.
     */
    @Before
    public void setUp() {
        initializeMockDaoFactory();
        exporter = new DeployedComponentExporter(mockDaoFactory);
    }

    /**
     * Initializes the mock data access object factory.
     */
    public void initializeMockDaoFactory() {
        mockDaoFactory = new MockDaoFactory();
        MockDeployedComponentDao deployedComponentDao = mockDaoFactory.getMockDeployedComponentDao();
        deployedComponentDao.save(createDeployedComponent("id1", "name1", "location1"));
    }

    /**
     * Verifies that the exporter works when some deployed components are matched.
     */
    @Test
    public void testExportWithResults() {
        JSONObject result = exporter.export("{\"id\":\"id1\"}");
        assertTrue(result.containsKey("components"));
        JSONArray matches = result.getJSONArray("components");
        assertEquals(1, matches.size());
        JSONObject match = matches.getJSONObject(0);
        assertEquals("id1", match.getString("id"));
        assertEquals("name1", match.getString("name"));
        assertEquals("location1", match.getString("location"));
    }

    /**
     * Verifies that the exporter works when no deployed components are matched.
     */
    @Test
    public void testExportWithoutResutls() {
        JSONObject result = exporter.export("{\"id\":\"idinfinity\"}");
        assertTrue(result.containsKey("components"));
        JSONArray matches = result.getJSONArray("components");
        assertEquals(0, matches.size());
    }
}
