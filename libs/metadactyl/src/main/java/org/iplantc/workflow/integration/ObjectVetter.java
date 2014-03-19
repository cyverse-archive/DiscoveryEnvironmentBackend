package org.iplantc.workflow.integration;

/**
 * Outlines functionality used when importing objects that replace other objects.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public interface ObjectVetter<T> {
    /**
     * Determines if an object has been vetted.  A vetted object is one that 
     * should not be replaced in the system.
     * 
     * @param username
     *  Fully qualified name of user.
     * @param obj
     *  Object to vet.
     * @return 
     *  True if it has been vetted, false otherwise.
     */
    public boolean isObjectVetted(String username, T obj);
}
