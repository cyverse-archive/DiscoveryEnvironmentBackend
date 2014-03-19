package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.TemplateGroupDao;
import org.iplantc.workflow.template.groups.TemplateGroup;

/**
 * Used to access persistent template groups in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateTemplateGroupDao extends HibernateGenericObjectDao<TemplateGroup> implements TemplateGroupDao {

    /**
     * @param session the database session.
     */
    public HibernateTemplateGroupDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateGroup findByHid(long hid) {
        String queryString = "from TemplateGroup where hid = :hid";
        Query query = getSession().createQuery(queryString);
        query.setLong("hid", hid);
        return (TemplateGroup) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateGroup findUniqueInstanceByName(String name) throws WorkflowException {
        List<TemplateGroup> templateGroups = super.findByName(name);
        if (templateGroups.size() > 1) {
            throw new WorkflowException("multiple template groups found with name: " + name);
        }
        return templateGroups.isEmpty() ? null : templateGroups.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<TemplateGroup> findTemplateGroupsContainingAnalysis(TransformationActivity analysis) {
        String queryString = "from TemplateGroup g where :analysis in elements(g.templates)";
        Query query = getSession().createQuery(queryString);
        query.setEntity("analysis", analysis);
        List<?> results = query.list();
        return (List<TemplateGroup>) results;
    }

    /** {@inheritDoc} */
    @Override
    public List<TemplateGroup> findTemplateGroupContainingSubgroup(TemplateGroup group) {
        String queryString = "from TemplateGroup g where :group in elements(g.sub_groups)";
        
        Query query = getSession().createQuery(queryString)
                                  .setParameter("group", group);
        
        return query.list();
    }
}
