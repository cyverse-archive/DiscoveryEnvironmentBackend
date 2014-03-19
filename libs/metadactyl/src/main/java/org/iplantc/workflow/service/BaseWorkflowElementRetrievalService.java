package org.iplantc.workflow.service;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.iplantc.persistence.RepresentableAsJson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A generalized service that can be used for retrieving workflow components.
 * 
 * @author Dennis Roberts
 */
public abstract class BaseWorkflowElementRetrievalService {

    /**
     * The Hibernate session factory.
     */
    protected SessionFactory sessionFactory;

    /**
     * The HQL query used to retrieve the list of components.
     */
    protected String queryString;

    /**
     * The name of the list in the resulting object.
     */
    protected String listName;

    /**
     * @param sessionFactory the new Hibernate session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param queryString the string to use to retrieve the list of workflow components.
     * @param listName the name of the list to use when building the result object.
     */
    protected BaseWorkflowElementRetrievalService(String queryString, String listName) {
        this.queryString = queryString;
        this.listName = listName;
    }

    /**
     * This is the service entry point. This method deals with all of the database details so that concrete sub-classes
     * can concentrate on actually retrieving and marshalling the components.
     * 
     * @return the list of workflow components.
     */
    public JSONObject retrieve() {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        JSONObject result = null;
        try {
            tx = session.beginTransaction();
            result = marshall(retrieveComponents(session));
            tx.commit();
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
        return result;
    }

    /**
     * Marshalls the result of this service.
     * 
     * @param components the components.
     * @return the marshalled result.
     */
    protected JSONObject marshall(List<RepresentableAsJson> components) {
        JSONObject result = new JSONObject();
        try {
            result.put(listName, marshallResultArray(components));
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Marshalls the result array.
     * 
     * @param components the list of components.
     * @return the marshalled list of components.
     */
    private JSONArray marshallResultArray(List<RepresentableAsJson> components) {
        JSONArray array = new JSONArray();
        for (RepresentableAsJson component : components) {
            array.put(component.toJson());
        }
        return array;
    }

    /**
     * Retrieves the marshalled list of workflow components.
     * 
     * @param session the Hibernate session.
     * @return the marshalled list of workflow components.
     */
    @SuppressWarnings("unchecked")
    private List<RepresentableAsJson> retrieveComponents(Session session) {
        return (List<RepresentableAsJson>) session.createQuery(queryString).list();
    }
}
