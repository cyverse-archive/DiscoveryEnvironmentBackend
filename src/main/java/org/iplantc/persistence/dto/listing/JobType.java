package org.iplantc.persistence.dto.listing;

/**
 * An enumeration of job types used in Metadactyl.
 * 
 * @author Dennis Roberts
 */
public enum JobType {

    EXECUTABLE("executable"),   // Jobs executed on our Condor cluster.
    FAPI("fAPI"),               // Jobs executed by the Foundational API.
    MIXED("mixed"),             // Multistep jobs with multiple job types.
    UNKNOWN("unknown");         // Jobs with unrecognized job types.

    /**
     * The string representation of the job type.
     */
    private String text;

    /**
     * @param text the string representation of the job type.
     */
    JobType(String text) {
        this.text = text;
    }
    
    /**
     * @return the string representation of the job type.
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Converts a String to a Job type.
     * 
     * @param text the string representation of the job type.
     * @return the job type.
     */
    public static JobType fromString(String text) {
        for (JobType jobType : JobType.values()) {
            if (jobType.text.equals(text)) {
                return jobType;
            }
        }
        return UNKNOWN;
    }
}
