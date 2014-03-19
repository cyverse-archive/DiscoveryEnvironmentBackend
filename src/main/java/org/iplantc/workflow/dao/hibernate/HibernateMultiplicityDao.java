package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.MultiplicityDao;
import org.iplantc.workflow.data.Multiplicity;

/**
 * Used to access persistent multiplicity instances in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateMultiplicityDao extends HibernateGenericObjectDao<Multiplicity> implements MultiplicityDao {

    /**
     * @param session the database session.
     */
    public HibernateMultiplicityDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multiplicity findUniqueInstanceByName(String name) throws WorkflowException {
        List<Multiplicity> multiplicities = findByName(name);
        if (multiplicities.size() > 1) {
            throw new WorkflowException("multiple multiplicities with the name \"" + name + "\" found");
        }
        return multiplicities.size() == 0 ? null : multiplicities.get(0);
    }
}
