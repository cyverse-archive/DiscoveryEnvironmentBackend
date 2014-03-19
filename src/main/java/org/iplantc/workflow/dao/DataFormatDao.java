package org.iplantc.workflow.dao;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.data.DataFormat;

/**
 * Used to access persistent data formats.
 * 
 * @author Dennis Roberts
 */
public interface DataFormatDao extends GenericDao<DataFormat> {
    /**
     * Finds the single data format with the given name.
     * 
     * @param name the data format name.
     * @return the data format.
     */
    public DataFormat findByName(String name);
}
