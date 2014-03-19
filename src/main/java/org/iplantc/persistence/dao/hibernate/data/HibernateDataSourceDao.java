package org.iplantc.persistence.dao.hibernate.data;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.data.DataSourceDao;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dto.data.DataSource;

/**
 * @author Dennis Roberts
 */
public class HibernateDataSourceDao extends AbstractHibernateDao<DataSource> implements DataSourceDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateDataSourceDao(Session session) {
        super(DataSource.class, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource findByName(String name) {
        Query query = getNamedQuery("findByName");
        query.setParameter("name", name);
        return (DataSource) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource findByUuid(String uuid) {
        Query query = getNamedQuery("findByUuid");
        query.setParameter("uuid", uuid);
        return (DataSource) query.uniqueResult();
    }
}
