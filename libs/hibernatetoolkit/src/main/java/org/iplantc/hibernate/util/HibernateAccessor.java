package org.iplantc.hibernate.util;

import org.hibernate.SessionFactory;

/**
 * Abstract base class for classes that need to access a database via a Hibernate session.
 * 
 * @author Dennis Roberts
 */
public abstract class HibernateAccessor {

    /**
     * The Hibernate database session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * Protected constructor.
     */
    protected HibernateAccessor() {
    }

    /**
     * Sets the session factory.
     * 
     * @param sessionFactory the new session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Gets the session factory.
     * 
     * @return the session factory.
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
