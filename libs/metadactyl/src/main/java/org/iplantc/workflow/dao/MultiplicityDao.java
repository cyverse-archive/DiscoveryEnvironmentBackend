package org.iplantc.workflow.dao;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.data.Multiplicity;

/**
 * Used to access persistent multiplicity instances.
 * 
 * @author Dennis Roberts
 */
public interface MultiplicityDao extends GenericObjectDao<Multiplicity> {

    /**
     * Finds the single multiplicity instance with the given name.
     * 
     * @param name the name of the multiplicity instance.
     * @return the multiplicity instance.
     * @throws WorkflowException if an error occurs.
     */
    public Multiplicity findUniqueInstanceByName(String name) throws WorkflowException;
}
