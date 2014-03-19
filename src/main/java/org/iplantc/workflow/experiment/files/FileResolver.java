package org.iplantc.workflow.experiment.files;

/**
 * Used to resolve special types of files that can be selected from a list.
 * 
 * @author Dennis Roberts
 */
public interface FileResolver {

    /**
     * Obtains the file access URL for a universally unique file identifier.
     * 
     * @param uuid the file identifier.
     * @return the URL used to access the file as a string.
     */
    public String getFileAccessUrl(String uuid);
}
