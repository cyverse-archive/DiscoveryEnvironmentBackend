package org.iplantc.workflow.dao;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.model.ValueType;

/**
 * Used to access persistent value types.
 * 
 * @author Dennis Roberts
 */
public interface ValueTypeDao extends GenericObjectDao<ValueType> {

    /**
     * Finds the single value type with the given name.
     * 
     * @param name the name of the value type.
     * @return the value type.
     * @throws WorkflowException if more than one value type has the given name.
     */
    public ValueType findUniqueInstanceByName(String name) throws WorkflowException;
}
