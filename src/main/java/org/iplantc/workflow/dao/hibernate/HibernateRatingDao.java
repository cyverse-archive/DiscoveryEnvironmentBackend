package org.iplantc.workflow.dao.hibernate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.workflow.core.Rating;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.RatingDao;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class HibernateRatingDao extends AbstractHibernateDao<Rating> implements RatingDao {
    public HibernateRatingDao(Session session) {
        super(Rating.class, session);
    }

    @Override
    public Double getVoteAverageForTransformationActivity(TransformationActivity transformationActivity) {
        Query query = getSession().createSQLQuery("SELECT AVG(rating) FROM ratings WHERE transformation_activity_id = :transformation_activity_id");
        query.setParameter("transformation_activity_id", transformationActivity.getHid());
        
        // The result will come back as a big decimal.  We don't need this kindof precision so cut
        // it down to a Double.
        BigDecimal result = (BigDecimal)query.uniqueResult();
        
        if(result == null) {
            return 0.0;
        } else {
            // Round to 2 decimal places.
            return result.round(new MathContext(3)).doubleValue();
        }
    }

    @Override
    public Rating findByUserAndTransformationActivity(User user, TransformationActivity transformationActivity) {
        Query query = getSession().createQuery("FROM Rating WHERE user = :user and transformationActivity = :transformationActivity");
        query.setParameter("user", user);
        query.setParameter("transformationActivity", transformationActivity);
        
        return (Rating)query.uniqueResult();
    }

    @Override
    public List<Rating> findByUser(User user) {
        Query query = getNamedQuery("findByUser");
        query.setParameter("user", user);
        
        return query.list();
    }
}
