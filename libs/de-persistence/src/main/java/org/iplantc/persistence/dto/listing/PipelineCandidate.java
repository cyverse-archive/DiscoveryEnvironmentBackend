package org.iplantc.persistence.dto.listing;

/**
 * Represents an analysis that can be checked to see if it qualifies for inclusion in a pipeline.
 * 
 * @author Dennis Roberts
 */
public interface PipelineCandidate {

    /**
     * @return the analysis identifier.
     */
    public String getId();

    /**
     * @return the number of steps in the analysis.
     */
    public long getStepCount();

    /**
     * @return the overall job type of the analysis.
     */
    public JobType getOverallJobType();
}
