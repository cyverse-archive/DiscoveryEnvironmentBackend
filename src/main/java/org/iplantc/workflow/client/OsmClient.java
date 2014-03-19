package org.iplantc.workflow.client;

import java.util.Map;

import net.sf.json.JSONObject;

import org.iplantc.workflow.WorkflowException;

/**
 * A client for communicating with the OSM.
 * 
 * @author Dennis Roberts
 */
public class OsmClient extends AbstractHttpClient {

    /**
     * The bucket to query.
     */
    String bucket;

    /**
     * @param bucket the new bucket.
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * @return the bucket.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Sends a query to the OSM and returns the results.
     * 
     * @param queryObject the object representing the query.
     * @throws WorkflowException if an error occurs.
     */
    public JSONObject query(JSONObject queryObject) {
        return query(queryObject, null);
    }

    /**
     * Sends a query to the OSM and returns the results.
     * 
     * @param queryObject the object representing the query.
     * @param options the query options.
     * @throws WorkflowException if an error occurs.
     */
    public JSONObject query(JSONObject queryObject, Map<String, String> options) {
        return postWithJsonResponse(createRequestUrl("query", options), queryObject);
    }

    /**
     * Saves an object in the OSM and returns its object persistence UUID.
     * 
     * @param object the object to save in the OSM.
     * @return the object persistence UUID.
     */
    public String save(JSONObject object) {
        return save(object, null);
    }

    /**
     * Saves an object in the OSM and returns its object persistence UUID.
     * 
     * @param object the object to save in the OSM.
     * @param options the query options.
     * @return the object persistence UUID.
     */
    public String save(JSONObject object, Map<String, String> options) {
        return postWithStringResponse(createRequestUrl(options), object);
    }

    /**
     * Creates a URL for a request to be sent to the OSM.
     * 
     * @param optionsMap the options to use for the request.
     * @return the URL.
     */
    private String createRequestUrl(Map<String, String> optionsMap) {
        return super.createRequestUrl(bucket, optionsMap);
    }

    /**
     * Creates a URL for a request to be sent to the OSM.
     * 
     * @param relativeUrl the relative URL for the specific request.
     * @param optionsMap the options to use for the request.
     * @return the URL.
     */
    @Override
    protected String createRequestUrl(String relativeUrl, Map<String, String> optionsMap) {
        return super.createRequestUrl(concatenatePaths(bucket, relativeUrl), optionsMap);
    }
}
