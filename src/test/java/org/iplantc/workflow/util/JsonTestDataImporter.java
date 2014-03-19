package org.iplantc.workflow.util;

import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class JsonTestDataImporter {
    public static String getRawTestJSONString(String name) throws IOException {
        InputStream in = Object.class.getResourceAsStream("/json/" + name + ".json");
        return IOUtils.toString(in);
    }
    
    /**
     * Gets the test json data under resources.  This is convention based - it
     * pulls the json from src/test/resources/json/&lt;name&gt;.json
     * @param name
     *  Name of the test data.  Exclude the .json
     * @return
     *  Parsed json data.
     * @throws IOException
     * @throws JSONException 
     */
    public static JSONObject getTestJSONObject(String name) throws IOException, JSONException {
        JSONObject json = new JSONObject(getRawTestJSONString(name));
        return json;
    }
    
    public static JSONArray getTestJSONArray(String name) throws IOException, JSONException {
        JSONArray json = new JSONArray(getRawTestJSONString(name));
        return json;
    }
}
