package org.iplantc.persistence.dao.hibernate.listing;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dao.listing.AnalysisGroupDao;
import org.iplantc.persistence.dto.listing.AnalysisGroup;

/**
 * @author Dennis Roberts
 */
public class HibernateAnalysisGroupDao extends AbstractHibernateDao<AnalysisGroup> implements AnalysisGroupDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateAnalysisGroupDao(Session session) {
         super(AnalysisGroup.class, session);
    }

    /**
     * @param id the external analysis group identifier.
     * 
     * @return the analysis group or null if the analysis group isn't found.
     */
    @Override
    public AnalysisGroup findByExternalId(String id) {
        Query query = getNamedQuery("findByExternalId");
        query.setParameter("id", id);
        return (AnalysisGroup) query.uniqueResult();
    }
}
