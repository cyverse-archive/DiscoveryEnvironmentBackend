package org.iplantc.hibernate.util;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;

/**
 * Times a task that is wrapped in a Hibernate session.
 * 
 * @author Dennis Roberts
 */
public class TimedSessionTaskWrapper extends SessionTaskWrapper {

    /**
     * Used to log the amount of time that it took to perform the task.
     */
    private static Logger LOG = Logger.getLogger(TimedSessionTaskWrapper.class);

    /**
     * The description of the task.
     */
    private String description;

    /**
     * @param sessionFactory the Hibernate session factory.
     * @param description a description of the task being performed.
     */
    public TimedSessionTaskWrapper(SessionFactory sessionFactory, String description) {
        super(sessionFactory);
        this.description = description;
    }

    /**
     * Performs a task and logs the amount of time that the task took.
     * 
     * @param <T> the return type of the task.
     * @param task the task.
     * @return the result of performing the task.
     */
    @Override
    public <T> T performTask(SessionTask<T> task) {
        long startTime = System.nanoTime();
        long endTime;
        try {
            return super.performTask(task);
        }
        finally {
            endTime = System.nanoTime();
            long duration = endTime - startTime;
            LOG.warn(description + " took " + ((double) duration / Math.pow(10, 9)) + " seconds");
        }
    }
}
