package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.PropertyTypeDao;
import org.iplantc.workflow.model.PropertyType;

/**
 * Used to access persistent property types in the database.
 * 
 * @author Dennis Roberts
 *
 */
public class HibernatePropertyTypeDao extends HibernateGenericObjectDao<PropertyType> implements PropertyTypeDao {

    /**
     * @param session the database session.
     */
    public HibernatePropertyTypeDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyType findUniqueInstanceByName(String name) throws WorkflowException {
        List<PropertyType> propertyTypes = super.findByName(name);
        if (propertyTypes.size() > 1) {
            throw new WorkflowException("multiple property types found with name: " + name);
        }
        return propertyTypes.size() == 0 ? null : propertyTypes.get(0);
    }
}
