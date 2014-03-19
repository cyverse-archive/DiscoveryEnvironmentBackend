package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.ValueTypeDao;
import org.iplantc.workflow.model.ValueType;

/**
 * Used to access persistent value types in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateValueTypeDao extends HibernateGenericObjectDao<ValueType> implements ValueTypeDao {

    /**
     * @param session the database session.
     */
    public HibernateValueTypeDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueType findUniqueInstanceByName(String name) throws WorkflowException {
        List<ValueType> valueTypes = super.findByName(name);
        if (valueTypes.size() > 1) {
            throw new WorkflowException("multiple value types found with name: " + name);
        }
        return valueTypes.size() == 0 ? null : valueTypes.get(0);
    }
}
