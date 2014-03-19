package org.iplantc.workflow.service;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.AnalysisNotFoundException;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.marshaler.UiAnalysisMarshaler;
import org.json.JSONException;

/**
 * A service that can be used to obtain analyses in the format expected by the UI.
 *
 * @author Dennis Roberts
 */
public class UiAnalysisService {

    /**
     * Used to obtain Hibernate sessions.
     */
    private SessionFactory sessionFactory;

    /**
     * @param sessionFactory used to obtain Hibernate sessions.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Obtains the JSON representing an analysis in the format expected by the UI.
     *
     * @param id the analysis ID.
     * @return the JSON representing the analysis.
     */
    public String getAnalysis(final String id) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return marshalAnalysis(new HibernateDaoFactory(session), id);
            }
        });
    }

    /**
     * Obtains the JSON representing an analysis in the format expected by the UI.
     *
     * @param daoFactory used to obtain data access objects.
     * @param id the analysis ID.
     * @return the JSON representing the analysis.
     */
    private String marshalAnalysis(DaoFactory daoFactory, String id) {
        try {
            return new UiAnalysisMarshaler(daoFactory).marshal(loadAnalysis(daoFactory, id)).toString();
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to format analysis " + id, e);
        }
    }

    /**
     * Loads an analysis from the database.
     *
     * @param daoFactory used to obtain data access objects.
     * @param id the analysis id.
     * @return the analysis.
     * @throws AnalysisNotFoundException if the analysis can't be found.
     */
    private TransformationActivity loadAnalysis(DaoFactory daoFactory, String id) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(id);
        if (analysis == null) {
            throw new AnalysisNotFoundException(id);
        }
        return analysis;
    }
}
