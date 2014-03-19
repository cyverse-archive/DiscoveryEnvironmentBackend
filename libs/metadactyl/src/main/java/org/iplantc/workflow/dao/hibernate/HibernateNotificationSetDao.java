package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.template.notifications.NotificationSet;

/**
 * Used to access persistent notification sets in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateNotificationSetDao extends HibernateGenericObjectDao<NotificationSet> implements
    NotificationSetDao
{

    /**
     * @param session the database session.
     */
    public HibernateNotificationSetDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NotificationSet> findNotificationSetsForAnalysis(TransformationActivity analysis) {
        return findNotificationSetsForAnalysisId(analysis.getId());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<NotificationSet> findNotificationSetsForAnalysisId(String analysisId) {
        String queryString = "from NotificationSet where template_id = ?";
        Query query = getSession().createQuery(queryString);
        query.setString(0, analysisId);
        return (List<NotificationSet>) query.list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationSetsForAnalysis(TransformationActivity analysis) {
        deleteNotificationSetsForAnalysisId(analysis.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotificationSetsForAnalysisId(String analysisId) {
        deleteAll(findNotificationSetsForAnalysisId(analysisId));
    }
}
