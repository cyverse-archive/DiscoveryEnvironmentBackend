package org.iplantc.hibernate.util;

import org.hibernate.Session;

/**
 * A task to be wrapped in a Hibernate session.
 * 
 * @author Dennis Roberts
 */
public interface SessionTask<T> {

    /**
     * Performs a task.
     * 
     * @param session the Hibernate session.
     * @return the result of performing the task.
     */
    public T perform(Session session);
}
