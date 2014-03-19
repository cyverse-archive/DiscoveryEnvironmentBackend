package org.iplantc.workflow.dao;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.data.InfoType;

/**
 * Used to access persistent information types in the database.
 * 
 * @author Dennis Roberts
 */
public interface InfoTypeDao extends GenericObjectDao<InfoType> {

    /**
     * Finds the single information type with the given name.
     * 
     * @param name the information type name.
     * @return the information type or null if a matching information type isn't found.
     * @throws WorkflowException if multiple matching information types are found.
     */
    public InfoType findUniqueInstanceByName(String name) throws WorkflowException;
}
