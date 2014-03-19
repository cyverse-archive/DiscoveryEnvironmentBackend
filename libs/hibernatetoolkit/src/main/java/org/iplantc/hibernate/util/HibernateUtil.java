package org.iplantc.hibernate.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static String LIVE_HIBERNATE_CONFIG = "hibernate.cfg.xml";

    /**
     * The Hibernate session factory.
     */
    private static SessionFactory sessionFactory = null;

    /**
     * Builds the Hibernate session factory.
     * 
     * @param configFile the configuration file.
     * @return the new session factory.
     */
    private static SessionFactory buildSessionFactory(String configFile) {
        try {
            return new Configuration().configure(configFile).buildSessionFactory();
        }
        catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Sets the session factory for testing purposes.
     * 
     * @param newSessionFactory the new session factory.
     */
    public static void setSessionFactoryForTesting(SessionFactory newSessionFactory) {
        sessionFactory = newSessionFactory;
    }

    /**
     * Gets the session factory using the specified configuration file.
     * 
     * @return the session factory.
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory(LIVE_HIBERNATE_CONFIG);
        }
        return sessionFactory;
    }
    
    /**
     * Gets the session factory using the specified configuration file
     *  
     * @param configFile the name of the hibernate config file.
     * @return the session factory.
     */
    public static SessionFactory getSessionFactory(String configFile) {
    	if (sessionFactory == null) {
    		sessionFactory = buildSessionFactory(configFile);
    	}
    	return sessionFactory;
    }
    
    public static void shutdown(){
    	getSessionFactory().close();
    }
}
