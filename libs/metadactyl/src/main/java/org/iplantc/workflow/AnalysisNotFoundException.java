package org.iplantc.workflow;

/**
 * Thrown when a required analysis isn't found.
 *
 * @author Dennis Roberts
 */
public class AnalysisNotFoundException extends ElementNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Throws an exception for an analysis ID that isn't found.
     *
     * @param id the analysis ID.
     */
    public AnalysisNotFoundException(String id) {
        super("analysis", id);
    }
}
