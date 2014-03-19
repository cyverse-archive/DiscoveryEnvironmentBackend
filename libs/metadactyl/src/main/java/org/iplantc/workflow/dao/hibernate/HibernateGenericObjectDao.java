package org.iplantc.workflow.dao.hibernate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.workflow.dao.GenericObjectDao;

/**
 * Used to generically access persistent objects in the database. Many of the ideas in this class are borrowed from
 * http://community.jboss.org/wiki/GenericDataAccessObjects.
 * 
 * @author Dennis Roberts
 * 
 * @param <T extends > the type of object to save.
 */
public abstract class HibernateGenericObjectDao<T extends NamedAndUnique> implements GenericObjectDao<T> {

    /**
     * The database session.
     */
    private Session session;

    /**
     * The name of the persistent class.
     */
    private Class<T> persistentClass;

    /**
     * @return the database session.
     */
    protected Session getSession() {
        return session;
    }

    /**
     * @return the persistent class.
     */
    protected Class<T> getPersistentClass() {
        return persistentClass;
    }

    /**
     * Initializes a new instance of this class.
     * 
     * @param session the database session.
     */
    public HibernateGenericObjectDao(Session session) {
        this.session = session;
        determinePersistentClass();
    }

    /**
     * Determines the name of the persistent class.
     */
    @SuppressWarnings("unchecked")
    private void determinePersistentClass() {
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new RuntimeException("missing type parameter");
        }
        persistentClass = (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
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
    public void deleteById(String id) {
        T object = findById(id);
        if (object != null) {
            delete(object);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll() {
        String className = persistentClass.getSimpleName();
        Query query = session.createQuery("from " + className);
        return (List<T>) query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public T findById(String id) {
        String className = persistentClass.getSimpleName();
        Query query = session.createQuery("from " + className + " where id = ?");
        query.setString(0, id);
        return (T) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<T> findByName(String name) {
        String className = persistentClass.getSimpleName();
        Query query = session.createQuery("from " + className + " where name = ?");
        query.setString(0, name);
        return (List<T>) query.list();
    }
}
