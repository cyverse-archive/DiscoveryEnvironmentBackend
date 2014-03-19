package org.iplantc.workflow.experiment;

/**
 * Used to assemble URLs.
 * 
 * @author Dennis Roberts
 */
public interface UrlAssembler {

    /**
     * Assembles a URL for the given path.
     * 
     * @param path the path to the resource.
     * @return the string representation of the URL.
     */
    public String assembleUrl(String path);
}
