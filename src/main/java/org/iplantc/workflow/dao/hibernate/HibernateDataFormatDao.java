package org.iplantc.workflow.dao.hibernate;

import org.hibernate.Query;

import org.hibernate.Session;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DataFormatDao;

/**
 * Used to access persistent data formats in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateDataFormatDao extends AbstractHibernateDao<DataFormat> implements DataFormatDao {

    /**
     * @param session the database session.
     */
    public HibernateDataFormatDao(Session session) {
        super(DataFormat.class, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataFormat findByName(String name) {
        Query query = getNamedQuery("findByName");
        query.setParameter("name", name);
        
        return (DataFormat)query.uniqueResult();
    }
}
