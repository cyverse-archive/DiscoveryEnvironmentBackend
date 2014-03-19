package org.iplantc.workflow.integration.json;

/**
 * An identifier retention strategy that always retains identifiers.
 * 
 * @author Dennis Roberts
 */
public class CopyIdRetentionStrategy implements IdRetentionStrategy {

    /**
     * Gets the identifier to use for an exported object.  For this strategy, the ID is always retained, so this method
     * merely returns the original ID.
     * 
     * @param originalId the original identifier.
     * @return the original identifier.
     */
    @Override
    public String getId(String originalId) {
        return originalId;
    }
}
