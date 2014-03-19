package org.iplantc.workflow.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.iplantc.workflow.WorkflowException;

/**
 * An abstract base class for HTTP clients that expect JSON responses.
 * 
 * @author Dennis Roberts
 */
public class AbstractHttpClient {

    /**
     * Used to log error and informational messages.
     */
    private static final Logger LOG = Logger.getLogger(AbstractHttpClient.class);

    /**
     * The number of characters to indent nested elements in logged JSON strings.
     */
    private static final int JSON_INDENT = 4;

    /**
     * The number of milliseconds that we're willing to wait for a connection by default.
     */
    protected static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    /**
     * The character encoding to use by default.
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * The base URL used to connect to the server.
     */
    private String baseUrl;

    /**
     * the number of milliseconds that we're willing to wait for a connection.
     */
    protected int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    /**
     * The character encoding to use when preparing requests.
     */
    protected String encoding = DEFAULT_ENCODING;

    /**
     * @param baseUrl the new base URL.
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @return the base URL.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param connectionTimeout the new connection timeout.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @return the connection timeout.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param encoding the new encoding.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sends a GET request to the server.
     * 
     * @param url the URL to send the request to.
     * @return the HTTP response body.
     */
    protected JSONObject getWithJsonResponse(String url) {
        return sendDisembodiedRequestWithJsonResponse(new HttpGet(url));
    }

    /**
     * Sends a GET request to the server.
     * 
     * @param url the URL to send the request to.
     * @return the HTTP response body.
     */
    protected String getWithStringResponse(String url) {
        return sendDisembodiedRequestWithStringResponse(new HttpGet(url));
    }

    /**
     * Sends a DELETE request to the server.
     * 
     * @param url the URL to send the request to.
     * @return the HTTP response body.
     */
    protected JSONObject deleteWithJsonResponse(String url) {
        return sendDisembodiedRequestWithJsonResponse(new HttpDelete(url));
    }

    /**
     * Sends a DELETE request to the server.
     * 
     * @param url the URL to send the request to.
     * @return the HTTP response body.
     */
    protected String deleteWithStringResponse(String url) {
        return sendDisembodiedRequestWithStringResponse(new HttpDelete(url));
    }

    /**
     * Sends a POST request to the server.
     * 
     * @param url the URL to send the request to.
     * @param body the request body.
     * @return the HTTP response body.
     */
    protected JSONObject postWithJsonResponse(String url, JSONObject body) {
        return sendEmbodiedRequestWithJsonResponse(new HttpPost(url), body);
    }

    /**
     * Sends a POST request to the server.
     * 
     * @param url the URL to send the request to.
     * @param body the request body.
     * @return the HTTP response body.
     */
    protected String postWithStringResponse(String url, JSONObject body) {
        return sendEmbodiedRequestWithStringResponse(new HttpPost(url), body);
    }

    /**
     * Sends a PUT request to the server.
     * 
     * @param url the URL to send the request to.
     * @param body the request body.
     * @return the HTTP response body.
     */
    protected JSONObject putWithJsonResponse(String url, JSONObject body) {
        return sendEmbodiedRequestWithJsonResponse(new HttpPut(url), body);
    }

    /**
     * Sends a PUT request to the server.
     * 
     * @param url the URL to send the request to.
     * @param body the request body.
     * @return the HTTP response body.
     */
    protected String putWithStringResponse(String url, JSONObject body) {
        return sendEmbodiedRequestWithStringResponse(new HttpPut(url), body);
    }

    /**
     * Sends an HTTP request with a body to the server and returns the response as a JSON object.
     * 
     * @param request the request.
     * @param body the request body.
     * @return the response as a JSON object.
     */
    private JSONObject sendEmbodiedRequestWithJsonResponse(HttpEntityEnclosingRequestBase request, JSONObject body) {
        logRequest(request, body);
        request.getMethod();
        HttpClient client = createHttpClient();
        try {
            JSONObject retval = jsonObjectFromString(sendRequestWithBody(client, request, body));
            logResponse(retval);
            return retval;
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Sends an HTTP request with a body to the server and returns the response as a string.
     * 
     * @param request the request.
     * @param body the request body.
     * @return the response as a string.
     */
    private String sendEmbodiedRequestWithStringResponse(HttpEntityEnclosingRequestBase request, JSONObject body) {
        logRequest(request, body);
        HttpClient client = createHttpClient();
        try {
            String retval = sendRequestWithBody(client, request, body);
            logResponse(retval);
            return retval;
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Sends an HTTP request with no body to the server and returns the response as a JSON object.
     * 
     * @param request the request.
     * @return the response as a JSON object.
     */
    private JSONObject sendDisembodiedRequestWithJsonResponse(HttpRequestBase request) {
        logRequest(request);
        HttpClient client = createHttpClient();
        try {
            JSONObject retval = jsonObjectFromString(sendRequest(client, request));
            logResponse(retval);
            return retval;
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Sends an HTTP request with no body to the server and returns the response as a String.
     * 
     * @param request the request.
     * @return the response as a String.
     */
    private String sendDisembodiedRequestWithStringResponse(HttpRequestBase request) {
        logRequest(request);
        HttpClient client = createHttpClient();
        try {
            String retval = sendRequest(client, request);
            logResponse(retval);
            return retval;
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Sends a request containing a body to the server.
     * 
     * @param client the HTTP client to use for the request.
     * @param request the request to send.
     * @param body the request body.
     * @return the response body.
     */
    private String sendRequestWithBody(HttpClient client, HttpEntityEnclosingRequestBase request, JSONObject body) {
        try {
            request.setEntity(new StringEntity(body.toString(), "application/json", encoding));
            return sendRequest(client, request);
        }
        catch (IOException e) {
            throw new WorkflowException("unable to prepare request", e);
        }
    }

    /**
     * Sends a request to the server.
     * 
     * @param client the HTTP client to use for the request.
     * @param request the request to send.
     * @return the response body.
     */
    private String sendRequest(HttpClient client, HttpRequestBase request) {
        try {
            HttpResponse response = client.execute(request);
            String responseBody = IOUtils.toString(response.getEntity().getContent());
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                throw new WorkflowException("server returned " + responseCode + " " + responseBody);
            }
            return responseBody;
        }
        catch (IOException e) {
            throw new WorkflowException("request failed", e);
        }
    }

    /**
     * Converts a string to a JSON object.
     * 
     * @param string the string to convert.
     * @return the JSON object.
     */
    private JSONObject jsonObjectFromString(String string) {
        JSON json = JSONSerializer.toJSON(string);
        if (!(json instanceof JSONObject)) {
            throw new WorkflowException("unexpected response from server: " + string);
        }
        return (JSONObject) json;
    }

    /**
     * Creates an HTTP client with the configured connection timeout.
     * 
     * @return the HTTP client.
     */
    private HttpClient createHttpClient() {
        HttpClient client = new DefaultHttpClient();
        client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        LOG.debug("created an HTTP client with a connection timeout of " + connectionTimeout + " milliseconds");
        return client;
    }

    /**
     * Logs a request that is being sent to the server.
     * 
     * @param request the request.
     */
    private void logRequest(HttpRequestBase request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sending a " + request.getMethod() + " request to " + request.getURI().toString());
        }
    }

    /**
     * Logs a request that is being sent to the server.
     * 
     * @param request the request.
     * @param body the request body.
     */
    private void logRequest(HttpEntityEnclosingRequestBase request, JSONObject body) {
        if (LOG.isDebugEnabled()) {
            logRequest(request, body.toString(JSON_INDENT));
        }
    }
    
    /**
     * Logs a request that is being sent to the server.
     * 
     * @param request the request.
     * @param body the request body.
     */
    private void logRequest(HttpEntityEnclosingRequestBase request, String body) {
        if (LOG.isDebugEnabled()) {
            logRequest(request);
            if (body != null) {
                LOG.debug("request body: " + body);
            }
        }
    }

    /**
     * Logs a response that was received from the server.
     * 
     * @param body the response body.
     */
    private void logResponse(JSONObject body) {
        if (LOG.isDebugEnabled()) {
            logResponse(body.toString(JSON_INDENT));
        }
    }

    /**
     * Logs a response that was received from the server.
     * 
     * @param body the response body.
     */
    private void logResponse(String body) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("response body: " + body);
        }
    }

    /**
     * Concatenates multiple paths into a single path.
     * 
     * @param paths the paths to concatenate.
     * @return the concatenated path.
     */
    protected String concatenatePaths(String... paths) {
        StringBuilder builder = new StringBuilder();
        for (String path : paths) {
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '/' && !path.startsWith("/")) {
                builder.append('/');
            }
            builder.append(path);
        }
        return builder.toString();
    }

    /**
     * Creates a URL for a request to be sent to the server.
     * 
     * @param relativeUrl the relative URL for the specific request.
     * @param optionsMap the options to use for the request.
     * @return the URL.
     */
    protected String createRequestUrl(String relativeUrl, Map<String, String> optionsMap) {
        StringBuilder builder = new StringBuilder(getBaseUrl() + "/" + relativeUrl);
        if (optionsMap != null && !optionsMap.isEmpty()) {
            builder.append("?").append(optionsMapToOptions(optionsMap));
        }
        return builder.toString();
    }

    /**
     * Converts an options map to a URL query string that can be placed in a URL.
     * 
     * @param optionsMap the options map.
     * @return the query string.
     */
    private String optionsMapToOptions(Map<String, String> optionsMap) {
        List<String> options = new LinkedList<String>();
        for (String option : optionsMap.keySet()) {
            String value = optionsMap.get(option);
            options.add(encode(option) + '=' + encode(value));
        }
        return StringUtils.join(options, '&');
    }

    /**
     * URL encodes a string using the default encoding.
     * 
     * @param str the string to encode.
     * @return the encoded string.
     */
    private String encode(String str) {
        try {
            return URLEncoder.encode(str, encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new WorkflowException("unable to build request URL", e);
        }
    }
}
