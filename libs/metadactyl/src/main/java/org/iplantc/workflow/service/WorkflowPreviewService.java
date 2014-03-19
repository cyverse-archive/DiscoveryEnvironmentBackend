package org.iplantc.workflow.service;

import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.integration.preview.WorkflowPreviewer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A service used to convert workflows in the import format to workflows in the format required by the UI.
 *
 * @author Dennis Roberts
 */
public class WorkflowPreviewService {

    /**
     * The database session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * @param sessionFactory the database session factory.
     * @throws IllegalArgumentException if one of the arguments is null.
     */
    public WorkflowPreviewService(SessionFactory sessionFactory) {
        Validate.notNull(sessionFactory, "missing required argument: sessionFactory");
        this.sessionFactory = sessionFactory;
    }

    /**
     * Converts the given JSON string from the format consumed by the workflow import service to the format
     * required by the Discovery Environment UI.
     *
     * @param jsonString the original JSON string.
     * @return the converted JSON string.
     * @throws JSONException if the JSONString is invalid or doesn't meet the requirements.
     */
    public String previewWorkflow(final String jsonString) throws JSONException {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
           @Override
            public String perform(Session session) {
                try {
                    WorkflowPreviewer previewer = createPreviewer(session);
                    return previewer.preview(new JSONObject(jsonString)).toString();
                }
                catch (JSONException e) {
                    throw new WorkflowException(e);
                }
            }
        });
    }

    /**
     * Converts the given JSON string from the format consumed by the workflow import service to the format required
     * by the Discovery Environment UI.
     *
     * @param jsonString the original JSON string.
     * @return the converted JSON string.
     * @throws JSONException if the JSONString is invalid or doesn't meet the requirements.
     */
    public String previewTemplate(final String jsonString) throws JSONException {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                try {
                    WorkflowPreviewer previewer = createPreviewer(session);
                    return previewer.previewTemplate(new JSONObject(jsonString)).toString();
                }
                catch (JSONException e) {
                    throw new WorkflowException(e);
                }
            }
        });
    }

    /**
     * Creates the object used to generate the preview JSON.
     *
     * @param session the database session.
     * @return the previewer.
     */
    private WorkflowPreviewer createPreviewer(Session session) {
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        WorkflowPreviewer previewer = new WorkflowPreviewer(daoFactory);
        return previewer;
    }
}
