package org.iplantc.workflow.service;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.client.OsmClient;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.experiment.PropertyValueRetriever;

/**
 * A service that can be used to get property values for a job submission request.
 * 
 * @author Dennis Roberts
 */
public class PropertyValueService {

    /**
     * Used to obtain Hibernate sessions.
     */
    private SessionFactory sessionFactory;

    /**
     * Used to communicate with the OSM.
     */
    private OsmClient osmClient;

    /**
     * @param sessionFactory used to obtain Hibernate sessions.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param osmClient used to communicate with the OSM.
     */
    public void setOsmClient(OsmClient osmClient) {
        this.osmClient = osmClient;
    }

    /**
     * Gets the property values for a previously submitted job.
     * 
     * @param jobUuid the job identifier.
     * @return 
     */
    public String getPropertyValues(final String jobUuid) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                DaoFactory daoFactory = new HibernateDaoFactory(session);
                return new PropertyValueRetriever(osmClient, daoFactory).getPropertyValues(jobUuid).toString();
            }
        });
    }
}
