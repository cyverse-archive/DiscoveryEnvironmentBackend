package org.iplantc.workflow.experiment;

import net.sf.json.JSONObject;

/**
 * Used to format job submission requests.
 * 
 * @author Dennis Roberts
 */
public interface JobRequestFormatter {

    /**
     * Formats a job request for submission to one of our job execution frameworks.
     * 
     * @return the formatted job request.
     */
    public JSONObject formatJobRequest();
}
