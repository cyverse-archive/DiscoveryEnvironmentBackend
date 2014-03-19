package org.iplantc.workflow.integration.json;

/**
 * A strategy used for retaining identifiers in exported JSON objects.
 * 
 * @author Dennis Roberts
 */
public interface IdRetentionStrategy {

    /**
     * Gets the identifier to use in the exported JSON.
     * @param originalId
     * @return 
     */
    public String getId(String originalId);
}
