package org.iplantc.persistence.dao.hibernate.listing;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dao.listing.AnalysisListingDao;
import org.iplantc.persistence.dto.listing.AnalysisListing;

/**
 * @author Dennis Roberts
 */
public class HibernateAnalysisListingDao extends AbstractHibernateDao<AnalysisListing> implements AnalysisListingDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateAnalysisListingDao(Session session) {
        super(AnalysisListing.class, session);
    }

    /**
     * @param id the external analysis listing identifier.
     * @return the analysis listing.
     */
    @Override
    public AnalysisListing findByExternalId(String id) {
        Query query = getNamedQuery("findByExternalId");
        query.setParameter("id", id);
        return (AnalysisListing) query.uniqueResult();
    }
}
