package org.iplantc.workflow.integration.json;

import java.util.Set;
import junit.framework.Assert;
import org.iplantc.workflow.core.Rating;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.mock.MockWorkspaceInitializer;
import org.iplantc.workflow.service.UserService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Used to test the TitoAnalysisUnmarshaller object.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class TitoAnalysisUnmarshallerTest {
    private UserService userService;
    
    private TitoAnalysisUnmarshaller unmarshaller;
    
    public TitoAnalysisUnmarshallerTest() {
    }
    
    /**
     * Initializes the test and Mock objects.
     */
    @Before
    public void init() {
        initializeUserService();
        
        unmarshaller = new TitoAnalysisUnmarshaller(new MockDaoFactory(), null);
        unmarshaller.setWorkspaceInitializer(new MockWorkspaceInitializer(userService));
    }
    
    /**
     * Initializes the user service.
     */
    public void initializeUserService() {
        userService = new UserService();
        userService.setRootAnalysisGroup("Workspace");
    }
    
    public JSONObject createTestRatingData() throws JSONException {
        JSONObject test = new JSONObject();
        JSONArray ratingsArray = new JSONArray();
        
        JSONObject rating = new JSONObject();
        rating.put("rating", 3);
        rating.put("username", "ipctest");
        
        ratingsArray.put(rating);
        test.put("ratings", ratingsArray);
        
        return test;
    }

    @Test
    public void shouldUnmarshalAnalysisFields() throws JSONException {
        JSONObject json = createAnalysisJson();
        TransformationActivity analysis = unmarshaller.fromJson(json);
        Assert.assertEquals("analysisid", analysis.getId());
        Assert.assertEquals("analysisname", analysis.getName());
        Assert.assertEquals("analysisdescription", analysis.getDescription());
        Assert.assertEquals("analysistype", analysis.getType());
        Assert.assertEquals("analysiswikiurl", analysis.getWikiurl());
        Assert.assertEquals(0, analysis.getSteps().size());
    }

    /**
     * Tests the import of rating data when a user's workspace already exists.
     */
    @Test
    public void testRatingImportWithUser() throws JSONException {
        JSONObject testData = createTestRatingData();
        
        unmarshaller.getWorkspaceInitializer().initializeWorkspace(unmarshaller.getDaoFactory(), "ipctest");
        Set<Rating> ratings = unmarshaller.unmarshalRatings(testData);
        
        Assert.assertFalse(ratings.isEmpty());
    }
    
    /**
     * Tests the import of rating data when a user's workspace has not yet
     * been created.
     */
    @Test
    public void testRatingImportWithoutUser() throws JSONException {
        JSONObject testData = createTestRatingData();
        
        Set<Rating> ratings = unmarshaller.unmarshalRatings(testData);
        
        Assert.assertFalse(ratings.isEmpty());
        Assert.assertNotNull(unmarshaller.getWorkspaceInitializer().getWorkspace(unmarshaller.getDaoFactory(), "ipctest"));
    }

    /**
     * Creates a JSON object representing an analysis.
     * 
     * @return the JSON object.
     * @throws JSONException if we try to use an invalid key name.
     */
    private JSONObject createAnalysisJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("analysis_id", "analysisid");
        json.put("analysis_name", "analysisname");
        json.put("description", "analysisdescription");
        json.put("type", "analysistype");
        json.put("wiki_url", "analysiswikiurl");
        json.put("steps", new JSONArray());
        json.put("implementation", createImplementationJson());
        return json;
    }

    /**
     * Creates a JSON object representing the implementation details of an analysis.
     * 
     * @return the JSON object.
     * @throws JSONException if we try to use an invalid key name.
     */
    private JSONObject createImplementationJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("implementor", "nobody");
        json.put("implementor_email", "nobody@iplantcollaborative.org");
        return json;
    }
}
