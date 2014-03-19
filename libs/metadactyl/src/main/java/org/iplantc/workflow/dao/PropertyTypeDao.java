package org.iplantc.workflow.dao;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.model.PropertyType;

/**
 * Used to access persistent property types.
 * 
 * @author Dennis Roberts
 */
public interface PropertyTypeDao extends GenericObjectDao<PropertyType> {

    /**
     * Finds the single property type with the given name.
     * 
     * @param name the name of the property type.
     * @return the property type.
     * @throws WorkflowException if multiple property types of the given name are found.
     */
    public PropertyType findUniqueInstanceByName(String name) throws WorkflowException;
}
