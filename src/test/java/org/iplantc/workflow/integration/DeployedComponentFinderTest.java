package org.iplantc.workflow.integration;

import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DeployedComponentDao;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.dao.mock.MockDeployedComponentDao;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;
import org.junit.Before;
import org.junit.Test;

import static org.iplantc.workflow.util.UnitTestUtils.createDeployedComponent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DeployedComponentFinder}.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentFinderTest {

    /**
     * The DAO factory to use for testing.
     */
    private MockDaoFactory mockDaoFactory;

    /**
     * The deployed component finder instance being tested.
     */
    private DeployedComponentFinder finder;

    /**
     * Initializes each unit test.
     */
    @Before
    public void setUp() {
        initializeMockDaoFactory();
        finder = new DeployedComponentFinder(mockDaoFactory);
    }

    /**
     * Initializes the mock data access object factory.
     */
    public void initializeMockDaoFactory() {
        mockDaoFactory = new MockDaoFactory();
        MockDeployedComponentDao deployedComponentDao = mockDaoFactory.getMockDeployedComponentDao();
        deployedComponentDao.save(createDeployedComponent("id1", "name1", "location1"));
        deployedComponentDao.save(createDeployedComponent("id2", "name2", "location1"));
        deployedComponentDao.save(createDeployedComponent("id3", "name1", "location3"));
    }

    /**
     * Verifies that we can search for deployed components by identifier.
     */
    @Test
    public void shouldSearchById() {
        validateResults(finder.search(buildCriteria("id1", null, null)), "id1");
        validateResults(finder.search(buildCriteria("id2", null, null)), "id2");
        validateResults(finder.search(buildCriteria("id3", null, null)), "id3");
        validateResults(finder.search(buildCriteria("id4", null, null)));
    }

    /**
     * Verifies that we can search for deployed components by name.
     */
    @Test
    public void shouldSearchByName() {
        validateResults(finder.search(buildCriteria(null, "name1", null)), "id1", "id3");
        validateResults(finder.search(buildCriteria(null, "name2", null)), "id2");
        validateResults(finder.search(buildCriteria(null, "name3", null)));
    }

    /**
     * Verifies that we can search for deployed components by location.
     */
    @Test
    public void shouldSearchByLocation() {
        validateResults(finder.search(buildCriteria(null, null, "location1")), "id1", "id2");
        validateResults(finder.search(buildCriteria(null, "location2", null)));
        validateResults(finder.search(buildCriteria(null, null, "location3")), "id3");
    }

    /**
     * Verifies that we can search for deployed components by name and location.
     */
    @Test
    public void shouldSearchByNameAndLocation() {
        validateResults(finder.search(buildCriteria(null, "name1", "location1")), "id1");
        validateResults(finder.search(buildCriteria(null, "name2", "location1")), "id2");
        validateResults(finder.search(buildCriteria(null, "name1", "location3")), "id3");
        validateResults(finder.search(buildCriteria(null, "name1", "location2")));
        validateResults(finder.search(buildCriteria(null, "name2", "location3")));
        validateResults(finder.search(buildCriteria(null, "name4", "location4")));
    }

    /**
     * Verifies that we can search for all deployed components.
     */
    @Test
    public void shouldSearchForAllDeployedComponents() {
        validateResults(finder.search(buildCriteria(null, null, null)), "id1", "id2", "id3");
    }

    /**
     * Verifies that we get an illegal argument exception when both the identifier and name are specified in the
     * search criteria.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldGetExceptionForIdAndName() {
        finder.search(buildCriteria("id1", "name1", null));
    }

    /**
     * Verifies that we get an illegal argument exception when both the identifier and location are specified in the
     * search criteria.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldGetExceptionForIdAndLocation() {
        finder.search(buildCriteria("id1", null, "location1"));
    }

    /**
     * Verifies that we get an illegal argument exception when the identifier, name and location are all specified in
     * the search criteria.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldGetExceptionForIdNameAndLocation() {
        finder.search(buildCriteria("id1", "name1", "location1"));
    }

    /**
     * Verifies that we get an illegal argument exception when an invalid JSON string is passed to the finder.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldGetExceptionForInvalidJson() {
        finder.search("");
    }

    /**
     * Validates the search results by comparing the expected results to the actual results.
     * 
     * @param expected the expected results.
     * @param actual the actual results.
     */
    private void validateResults(List<DeployedComponent> actual, String... expectedIds) {
        List<DeployedComponent> expected = getDeployedComponents(expectedIds);
        assertEquals(expected.size(), actual.size());
        for (DeployedComponent component : expected) {
            String msg = "search results contain deployed component " + component.getId();
            assertTrue(msg, actual.contains(component));
        }
    }

    /**
     * Gets the deployed components with the given identifiers.
     * 
     * @param ids the identifiers to search for.
     * @return the list of deployed components.
     * @throws WorkflowException if one of the identifiers is not found.
     */
    private List<DeployedComponent> getDeployedComponents(String... ids) {
        final DeployedComponentDao dao = mockDaoFactory.getDeployedComponentDao();
        return ListUtils.map(new Lambda<String, DeployedComponent>() {
            @Override
            public DeployedComponent call(String id) {
                DeployedComponent result = dao.findById(id);
                if (result == null) {
                    throw new WorkflowException("deployed component " + id + " not found");
                }
                return result;
            }
        }, Arrays.asList(ids));
    }

    /**
     * Builds the criteria for a deployed component search.
     * 
     * @param id the deployed component identifier.
     * @param name the deployed component name.
     * @param location the deployed component location.
     * @return the search criteria.
     */
    private String buildCriteria(String id, String name, String location) {
        JSONObject criteria = new JSONObject();
        criteria.put("id", id);
        criteria.put("name", name);
        criteria.put("location", location);
        return criteria.toString();
    }
}
