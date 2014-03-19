package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DeployedComponentDao;

/**
 * Used to access persistent deployed components in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateDeployedComponentDao extends HibernateGenericObjectDao<DeployedComponent> implements
        DeployedComponentDao {

    /**
     * @param session the database session.
     */
    public HibernateDeployedComponentDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeployedComponent findByNameAndLocation(String name, String location) {
        String queryString = "from DeployedComponent where name = ? and location = ?";
        Query query = getSession().createQuery(queryString);
        query.setString(0, name);
        query.setString(1, location);
        return (DeployedComponent) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeployedComponent findUniqueInstanceByName(String name) {
        List<DeployedComponent> components = findByName(name);
        if (components.size() > 1) {
            throw new WorkflowException("multiple deployed components found with name: " + name);
        }
        return components.isEmpty() ? null : components.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeployedComponent> findByLocation(String location) {
        String queryString = "from DeployedComponent where location = ?";
        Query query = getSession().createQuery(queryString);
        query.setString(0, location);
        return (List<DeployedComponent>) query.list();
    }
}
