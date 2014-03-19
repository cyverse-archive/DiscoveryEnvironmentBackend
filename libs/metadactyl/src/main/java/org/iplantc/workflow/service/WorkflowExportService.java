package org.iplantc.workflow.service;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.integration.AnalysisExporter;
import org.iplantc.workflow.integration.DeployedComponentExporter;
import org.iplantc.workflow.integration.TemplateExporter;

/**
 * A service that can be used to export analyses or templates.
 *
 * @author Dennis Roberts
 */
public class WorkflowExportService {

    /**
     * The database session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * @param sessionFactory the database session factory.
     */
    public WorkflowExportService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Exports a template.
     *
     * @param templateId the template identifier.
     * @return a JSON string representing the template.
     */
    public String exportTemplate(final String templateId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return getTemplate(new HibernateDaoFactory(session), templateId);
            }
        });
    }

    /**
     * Gets a template from the database.
     *
     * @param daoFactory used to obtain data access objects.
     * @param templateId the template identifier.
     * @return a JSON string representing the template.
     */
    public String getTemplate(DaoFactory daoFactory, String templateId) {
        return new TemplateExporter(daoFactory).exportTemplate(templateId).toString();
    }

    /**
     * Exports an analysis.
     *
     * @param analysisId the analysis identifier.
     * @return a JSON string representing the analysis,
     */
    public String exportAnalysis(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return getAnalysis(new HibernateDaoFactory(session), analysisId);
            }
        });
    }

    /**
     * Gets an analysis from the database.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the analysis identifier.
     * @return a JSON string representing the analysis.
     */
    public String getAnalysis(DaoFactory daoFactory, String analysisId) {
        return new AnalysisExporter(daoFactory).exportAnalysis(analysisId).toString();
    }

    /**
     * Exports deployed components based upon search criteria that can be specified inside a JSON object.
     *
     * @param criteria the JSON string representing the search criteria.
     * @return A JSON string representing the exported deployed components.
     */
    public String getDeployedComponents(final String criteria) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return new DeployedComponentExporter(new HibernateDaoFactory(session)).export(criteria).toString();
            }
        });
    }
}
