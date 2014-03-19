package org.iplantc.workflow.dao.hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.dao.TransformationActivityDao;

/**
 * Used to access persistent transformation activities in the database.
 *
 * @author Dennis Roberts
 */
public class HibernateTransformationActivityDao extends HibernateGenericObjectDao<TransformationActivity> implements
    TransformationActivityDao
{

    /**
     * @param session the database session.
     */
    public HibernateTransformationActivityDao(Session session) {
        super(session);
    }

    @Override
    public void delete(TransformationActivity analysis) {
        deleteNotificationSetsForAnalysis(analysis);
        getSession().delete(analysis);
    }

    /**
     * Deletes the notification sets associated with the given analysis.
     *
     * @param analysis the analysis.
     */
    private void deleteNotificationSetsForAnalysis(TransformationActivity analysis) {
        NotificationSetDao notificationSetDao = new HibernateNotificationSetDao(getSession());
        notificationSetDao.deleteNotificationSetsForAnalysis(analysis);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getTemplateIdsInAnalysis(TransformationActivity analysis) {
        String queryString = "select t.template_id "
            + "from TransformationActivity as a "
            + "join a.steps as s "
            + "join s.transformation as t "
            + "where a.id = ?";
        Query query = getSession().createQuery(queryString);
        query.setString(0, analysis.getId());
        return new HashSet<String>(query.list());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<TransformationActivity> getAnalysesReferencingTemplateId(String templateId) {
        String queryString = "from TransformationActivity a "
            + "inner join fetch a.steps s "
            + "inner join fetch s.transformation t "
            + "where t.template_id = ?";
        Query query = getSession().createQuery(queryString);
        query.setString(0, templateId);
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationActivity findUniqueInstanceByName(String name) {
        List<TransformationActivity> analyses = findByName(name);
        if (analyses.size() > 1) {
            throw new WorkflowException("multiple analyses found with name: " + name);
        }
        return analyses.isEmpty() ? null : analyses.get(0);
    }
}
