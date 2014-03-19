package org.iplantc.persistence.dao.hibernate;

import java.util.Collection;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.GenericDao;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public abstract class AbstractHibernateDao<T> implements GenericDao<T> {

    private Session session;

    private Class<T> dtoClass;

    public AbstractHibernateDao(Class<T> dtoClass, Session session) {
        this.dtoClass = dtoClass;
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    protected Class<T> getDTOClass() {
        return dtoClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(T object) {
        session.save(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(T object) {
        session.delete(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<T> objects) {
        for (T object : objects) {
            delete(object);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(long id) {
        T object = findById(id);
        if (object != null) {
            delete(object);
        }
    }

    public Query getNamedQuery(String queryName) {
        return session.getNamedQuery(dtoClass.getSimpleName() + "." + queryName);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public T findById(long id) {
        Query query = getNamedQuery("findById");
        query.setParameter("id", id);

        return (T) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll() {
        Query query = session.createQuery("from " + dtoClass.getSimpleName());
        return query.list();
    }
}
