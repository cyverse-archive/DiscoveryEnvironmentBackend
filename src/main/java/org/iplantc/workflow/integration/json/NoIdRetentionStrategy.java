package org.iplantc.workflow.integration.json;

import org.iplantc.workflow.integration.util.ImportUtils;

/**
 * An identifier retention strategy that never retains identifiers.
 * 
 * @author 
 */
public class NoIdRetentionStrategy implements IdRetentionStrategy {

    /**
     * Gets the identifier to use for an exported object.  For this strategy, the ID is never retained, so this class
     * simply generates and returns a new ID.
     * 
     * @param originalId the original identifier.
     * @return the new identifier.
     */
    @Override
    public String getId(String originalId) {
        return ImportUtils.generateId();
    }
}
