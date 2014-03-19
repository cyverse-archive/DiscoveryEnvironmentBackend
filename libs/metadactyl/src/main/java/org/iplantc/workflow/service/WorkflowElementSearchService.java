package org.iplantc.workflow.service;

import org.hibernate.SessionFactory;

/**
 * A general service used to search lists of workflow elements.
 *
 * @author psarando
 */
public class WorkflowElementSearchService {

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * Sets the session factory.
     *
     * @param sessionFactory the session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Searches the list of deployed components.
     *
     * @param searchTerm the term to search for.
     * @return the list of filtered deployed components.
     */
    public String searchDeployedComponents(String searchTerm) {
        DeployedComponentRetrievalService service = new DeployedComponentRetrievalService(sessionFactory);

        return service.searchComponents(searchTerm).toString();
    }

}
