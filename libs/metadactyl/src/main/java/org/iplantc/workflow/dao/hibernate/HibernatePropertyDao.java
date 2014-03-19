package org.iplantc.workflow.dao.hibernate;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.workflow.dao.PropertyDao;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;

/**
 * Used to access persistent properties in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernatePropertyDao extends HibernateGenericObjectDao<Property> implements PropertyDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernatePropertyDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getPropertyForDataObject(DataObject dataObject) {
        Query query = getSession().createQuery("from Property where dataObject = :dataObject");
        query.setParameter("dataObject", dataObject);
        return (Property) query.uniqueResult();
    }
}
