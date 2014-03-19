package org.iplantc.workflow.dao;

/**
 * Uses a DAO factory while performing one or more tasks.
 * 
 * @author Dennis Roberts
 */
public interface DaoFactoryUser<T> {

    /**
     * Use the given DAO factory to perform one or more tasks.
     * 
     * @param daoFactory the DAO factory.
     * @return the result of performing the tasks.
     */
    public T doTasks(DaoFactory daoFactory);
}
