package org.iplantc.workflow.service;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.iplantc.persistence.RepresentableAsJson;
import org.json.JSONObject;

/**
 * A service that can be used to retrieve a list of known deployed components.
 *
 * @author Dennis Roberts
 */
public class DeployedComponentRetrievalService extends BaseWorkflowElementRetrievalService {

    /**
     * Initializes the superclass with the appropriate query.
     */
    protected DeployedComponentRetrievalService() {
        super("from DeployedComponent", "components");
    }

    /**
     * Initializes the superclass with the appropriate query and sets the session factory.
     *
     * @param sessionFactory the session factory.
     */
    public DeployedComponentRetrievalService(SessionFactory sessionFactory) {
        this();
        setSessionFactory(sessionFactory);
    }

    /**
     * This is the service entry point. This method deals with all of the database details of retrieving
     * the list of filtered components and marshalling them into a JSONObject.
     *
     * @return the marshalled list of filtered deployed components.
     */
    public JSONObject searchComponents(String searchTerm) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        JSONObject result = null;

        try {
            tx = session.beginTransaction();

            result = marshall(filterComponents(session, searchTerm));

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }

            throw e;
        } finally {
            session.close();
        }

        return result;
    }

    /**
     * Retrieves a list of deployed components filtered by name or description.
     *
     * @param session the Hibernate session.
     * @param searchTerm the term used to filter the list of deployed components by name or description.
     * @return the list of filtered deployed components.
     */
    private List<RepresentableAsJson> filterComponents(Session session, String searchTerm) {
        String searchClause = "lower(%1$s) like '%%' || lower(:search) || '%%'";

        // Escape SQL wildcard characters.
        String escapedSearchTerm = searchTerm.replaceAll("([\\\\%_])", "\\\\$1");
        // Replace client wildcard characters with SQL wildcard characters.
        escapedSearchTerm = escapedSearchTerm.replace("*", "%").replace("?", "_");

        String filter = String.format("where %1$s OR %2$s",
                                      String.format(searchClause, "name"),
                                      String.format(searchClause, "description"));

        Query query = session.createQuery(queryString + " " + filter).setParameter("search", escapedSearchTerm);

        return (List<RepresentableAsJson>)query.list();
    }
}
