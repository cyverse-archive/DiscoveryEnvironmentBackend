package org.iplantc.hibernate.util;

import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Wraps a task in a Hibernate session.
 *
 * @author Dennis Roberts
 */
public class SessionTaskWrapper {

    /**
     * Used to log the next exception when there is one.
     */
    private static final Logger LOG = Logger.getLogger(SessionTaskWrapper.class);

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * @param sessionFactory the Hibernate session factory.
     */
    public SessionTaskWrapper(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Performs a task.
     *
     * @param <T> the return type of the task.
     * @param task the task.
     * @return the result of performing the task.
     */
    public <T> T performTask(SessionTask<T> task) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            T result = task.perform(session);
            tx.commit();
            return result;
        }
        catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            logNextException(e);
            throw e;
        }
        catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
        finally {
            session.close();
        }
    }

    /**
     * Log the result of calling nextException if the result is not null.
     *
     * @param e the source exception.
     */
    private void logNextException(HibernateException e) {
        for (Throwable currException = e; currException != null; currException = currException.getCause()) {
            if (currException instanceof SQLException) {
                logNextExceptionChain((SQLException) currException);
            }
        }
    }

    /**
     * Recursively logs all SQL exceptions in a chain obtained by repeatedly calling getNextException.
     *
     * @param currException the current exception.
     */
    private void logNextExceptionChain(SQLException currException) {
        SQLException nextException = currException.getNextException();
        if (nextException != null) {
            LOG.error("Next Exception for " + currException, nextException);
            logNextExceptionChain(nextException);
        }
    }
}
