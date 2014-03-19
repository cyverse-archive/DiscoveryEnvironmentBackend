package org.iplantc.workflow.service.dto;

import net.sf.json.JSONArray;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.CategoryPath.
 * 
 * @author Dennis Roberts
 */
public class CategoryPathTest {

    /**
     * Verifies that we can construct a category path from a username and path.
     */
    @Test
    public void testConstructionFromUsernameAndPath() {
        CategoryPath categoryPath = new CategoryPath("username", Arrays.asList("foo", "bar"));
        assertEquals("username", categoryPath.getUsername());
        assertEquals(Arrays.asList("foo", "bar"), categoryPath.getPath());
    }

    /**
     * Verifies that we can construct a category path from a JSON object.
     */
    @Test
    public void testConstructionFromJsonObject() {
        CategoryPath categoryPath = new CategoryPath(createJson("username", Arrays.asList("foo", "bar")));
        assertEquals("username", categoryPath.getUsername());
        assertEquals(Arrays.asList("foo", "bar"), categoryPath.getPath());
    }

    /**
     * Verifies that we can construct a category path from a string.
     */
    @Test
    public void testConstructionFromString() {
        CategoryPath categoryPath = new CategoryPath(createJson("username", Arrays.asList("foo", "bar")).toString());
        assertEquals("username", categoryPath.getUsername());
        assertEquals(Arrays.asList("foo", "bar"), categoryPath.getPath());
    }

    /**
     * Verifies that we can convert a category path to a JSON object.
     */
    @Test
    public void testJsonConstruction() {
        CategoryPath categoryPath = new CategoryPath("username", Arrays.asList("foo", "bar"));
        JSONObject json = createJson("username", Arrays.asList("foo", "bar"));
        assertEquals(json, categoryPath.toJson());
    }

    /**
     * Generates a JSON object that represents a category path.
     * 
     * @param username the name of the user that owns the workspace.
     * @param path the path to the category, relative to the user's workspace.
     * @return the JSON object.
     */
    private JSONObject createJson(String username, List<String> path) {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("path", JSONArray.fromObject(path));
        return json;
    }
}
