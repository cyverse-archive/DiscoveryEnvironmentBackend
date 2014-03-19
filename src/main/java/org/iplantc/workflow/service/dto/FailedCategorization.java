package org.iplantc.workflow.service.dto;

import net.sf.json.JSONObject;

/**
 * A data transfer object for a failed analysis categorization.
 * 
 * @author Dennis Roberts
 */
public class FailedCategorization extends AbstractDto {

    /**
     * The categorization that failed.
     */
    @JsonField(name = "categorization")
    protected CategorizedAnalysis categorization;

    /**
     * The reason for the failure.
     */
    @JsonField(name = "reason")
    protected String reason;

    /**
     * @return the categorization that failed.
     */
    public CategorizedAnalysis getCategorization() {
        return categorization;
    }

    /**
     * @param categorization the categorization that failed.
     */
    public void setCategorization(CategorizedAnalysis categorization) {
        this.categorization = categorization;
    }

    /**
     * @return the reason for the failure.
     */
    public String getReason() {
        return reason;
    }

    /**
     * @param reason the reason for the failure.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * @param categorization the categorization that failed.
     * @param t the exception that caused the failure.
     */
    public FailedCategorization(CategorizedAnalysis categorization, Throwable t) {
        this.categorization = categorization;
        this.reason = t.getMessage();
    }

    /**
     * @param json a JSON object representing a failed categorization.
     */
    public FailedCategorization(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str a JSON string representing a failed categorization.
     */
    public FailedCategorization(String str) {
        fromString(str);
    }
}
