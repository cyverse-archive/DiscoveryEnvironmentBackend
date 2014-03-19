package org.iplantc.persistence.dao.data;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.data.DataSource;

/**
 * @author Dennis Roberts
 */
public interface DataSourceDao extends GenericDao<DataSource> {

    /**
     * Finds a data source by name.
     *
     * @param name the name to search for.
     * @return the data source or null if a matching data source isn't found.
     */
    public DataSource findByName(String name);

    /**
     * Finds a data source by UUID.
     *
     * @param uuid the UUID to search for.
     * @return the data source or null if a matching data source isn't found.
     */
    public DataSource findByUuid(String uuid);
}
