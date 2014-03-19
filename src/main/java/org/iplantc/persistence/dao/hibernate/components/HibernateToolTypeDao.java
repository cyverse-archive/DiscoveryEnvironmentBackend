package org.iplantc.persistence.dao.hibernate.components;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.components.ToolTypeDao;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dto.components.ToolType;

/**
 * @author Dennis Roberts
 */
public class HibernateToolTypeDao extends AbstractHibernateDao<ToolType> implements ToolTypeDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateToolTypeDao(Session session) {
        super(ToolType.class, session);
    }

    /**
     * Finds the tool type with the given name.
     * 
     * @param name the tool type name.
     * @return the tool type or null if a match isn't found.
     */
    @Override
    public ToolType findByName(String name) {
        Query query = getNamedQuery("findByName");
        query.setParameter("name", name);
        return (ToolType) query.uniqueResult();
    }
}
