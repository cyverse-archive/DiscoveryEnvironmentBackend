package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.InfoTypeDao;
import org.iplantc.workflow.data.InfoType;

/**
 * Used to access persistent information types in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateInfoTypeDao extends HibernateGenericObjectDao<InfoType> implements InfoTypeDao {

    /**
     * @param session the database session.
     */
    public HibernateInfoTypeDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InfoType findUniqueInstanceByName(String name) throws WorkflowException {
        List<InfoType> infoTypes = super.findByName(name);
        if (infoTypes.size() > 1) {
            throw new WorkflowException("multiple information types found with name: " + name);
        }
        return infoTypes.size() == 0 ? null : infoTypes.get(0);
    }
}
