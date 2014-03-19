package org.iplantc.workflow.integration;

/**
 * Indicates what should be done when an existing workflow element matches a workflow element that is being imported.
 * 
 * @author Dennis Roberts
 */
public enum UpdateMode {
    IGNORE, REPLACE, THROW;
    
    /**
     * The default update mode.
     */
    public static final UpdateMode DEFAULT = THROW;
}
