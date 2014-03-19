package org.iplantc.persistence.dao.components;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.components.ToolType;

/**
 * @author Dennis Roberts
 */
public interface ToolTypeDao extends GenericDao<ToolType> {

    /**
     * Finds the tool type with the given name.  Tool types are uniquely identified by name.
     * 
     * @param name the name to search for.
     * @return the matching tool type or null if no match is found.
     */
    public ToolType findByName(String name);
}
