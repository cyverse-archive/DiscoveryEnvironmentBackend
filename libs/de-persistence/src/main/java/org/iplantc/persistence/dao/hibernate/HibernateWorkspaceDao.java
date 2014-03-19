package org.iplantc.persistence.dao.hibernate;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.WorkspaceDao;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;

/**
 * @author Dennis Roberts
 */
public class HibernateWorkspaceDao extends AbstractHibernateDao<Workspace> implements WorkspaceDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateWorkspaceDao(Session session) {
        super(Workspace.class, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByUser(User user) {
        Workspace workspace = findByUser(user);
        if (workspace != null) {
            delete(workspace);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workspace findByUser(User user) {
        Query query = getNamedQuery("findByUser");
        query.setParameter("user", user);
        return (Workspace) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Workspace> findPublicWorkspaces() {
        Query query = getNamedQuery("findPublicWorkspaces");
        return query.list();
    }
}
