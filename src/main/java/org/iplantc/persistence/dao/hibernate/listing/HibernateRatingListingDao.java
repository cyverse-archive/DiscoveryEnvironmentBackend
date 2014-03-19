package org.iplantc.persistence.dao.hibernate.listing;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.listing.RatingListingDao;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.persistence.dto.listing.RatingListing;
import org.iplantc.persistence.dto.user.User;

/**
 * A data access object that can be used to obtain analysis rating information from the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateRatingListingDao implements RatingListingDao {

    /**
     * The Hibernate session.
     */
    private Session session;

    /**
     * @param session the Hibernate session.
     */
    public HibernateRatingListingDao(Session session) {
        this.session = session;
    }

    /**
     * Gets a named query.
     * 
     * @param queryName the name of the query.
     * @return the query.
     */
    public Query getNamedQuery(String queryName) {
        return session.getNamedQuery(RatingListing.class.getSimpleName() + "." + queryName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RatingListing> findByUser(User user) {
        Query query = getNamedQuery("findByUser");
        query.setParameter("userId", user.getId());
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RatingListing findByUserAndAnalysisListing(User user, AnalysisListing analysisListing) {
        Query query = getNamedQuery("findByAnalysisAndUser");
        query.setParameter("analysisId", analysisListing.getHid());
        query.setParameter("userId", user.getId());
        return (RatingListing) query.uniqueResult();
    }
}
