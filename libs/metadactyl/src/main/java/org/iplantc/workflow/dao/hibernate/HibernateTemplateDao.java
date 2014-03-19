package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.TemplateDao;
import org.iplantc.workflow.model.Template;

/**
 * Used to access persistent templates in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateTemplateDao extends HibernateGenericObjectDao<Template> implements TemplateDao {

    /**
     * @param session the database session.
     */
    public HibernateTemplateDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Template findUniqueInstanceByName(String name) {
        List<Template> templates = findByName(name);
        if (templates.size() > 1) {
            throw new WorkflowException("multiple templates found with name: " + name);
        }
        return templates.isEmpty() ? null : templates.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Template> findTemplatesInAnalysis(TransformationActivity analysis) {
        String queryString = "from Template where id in ("
                + "select tr.template_id from TransformationActivity as ta "
                + "join ta.steps as ts "
                + "join ts.transformation as tr "
                + "where ta = :analysis)";
        Query query = getSession().createQuery(queryString);
        query.setEntity("analysis", analysis);
        return query.list();
    }
}
