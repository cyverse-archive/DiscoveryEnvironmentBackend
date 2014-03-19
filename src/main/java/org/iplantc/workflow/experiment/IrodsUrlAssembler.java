package org.iplantc.workflow.experiment;

/**
 * Used to assemble iRODS URLs.
 * 
 * @author Dennis Roberts
 */
public class IrodsUrlAssembler implements UrlAssembler {

    /**
     * {@inheritDoc}
     */
    @Override
    public String assembleUrl(String path) {
        return path;
    }
}
