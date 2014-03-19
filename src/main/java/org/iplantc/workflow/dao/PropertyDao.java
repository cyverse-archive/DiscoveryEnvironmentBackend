package org.iplantc.workflow.dao;

import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;

/**
 * Used to access persistent properties.
 * 
 * @author Dennis Roberts
 */
public interface PropertyDao extends GenericObjectDao<Property> {

    /**
     * Finds the property that references the data object if one exists.
     * 
     * @return the property or null if no property references the data object.
     */
    public Property getPropertyForDataObject(DataObject dataObject);
}
