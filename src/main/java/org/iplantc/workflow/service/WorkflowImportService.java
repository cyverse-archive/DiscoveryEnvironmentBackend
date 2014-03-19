package org.iplantc.workflow.service;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.UnknownUpdateModeException;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateNotificationSetDao;
import org.iplantc.workflow.integration.AnalysisGeneratingTemplateImporter;
import org.iplantc.workflow.integration.AnalysisImporter;
import org.iplantc.workflow.integration.AnalysisUpdater;
import org.iplantc.workflow.integration.DeployedComponentImporter;
import org.iplantc.workflow.integration.NotificationSetImporter;
import org.iplantc.workflow.integration.TemplateGroupImporter;
import org.iplantc.workflow.integration.TemplateImporter;
import org.iplantc.workflow.integration.UpdateMode;
import org.iplantc.workflow.integration.WorkflowImporter;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.integration.validation.TemplateValidator;
import org.iplantc.workflow.integration.validation.TemplateValidatorFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A service used to import workflows.
 *
 * @author Dennis Roberts
 */
public class WorkflowImportService {

    /**
     * Used to log error messages.
     */
    private static final Logger LOG = Logger.getLogger(WorkflowImportService.class);

    /**
     * The database session factory.
     */
    private final SessionFactory sessionFactory;

    /**
     * The index of the development analysis group.
     */
    private final int devAnalysisGroupIndex;

    /**
     * The index of the favorites analysis group.
     */
    private final int favoritesAnalysisGroupIndex;

    /**
     * Used to initialize the user's workspace if necessary.
     */
    private final WorkspaceInitializer workspaceInitializer;

    /**
     * Used to validate templates that are being imported.
     */
    private TemplateValidator templateValidator = TemplateValidatorFactory.createDefaultTemplateValidator();

    /**
     * Initializes a new workflow import service.
     *
     * @param sessionFactory the database sessionFactory.
     * @param devAnalysisGroupIndex the index of the analysis group used for tool development.
     * @param favoritesAnalysisGroupIndex the index of the analysis group used to store the user's favorites.
     * @param workspaceInitializer used to initialize the user's workspace if necessary.
     */
    public WorkflowImportService(SessionFactory sessionFactory, String devAnalysisGroupIndex,
            String favoritesAnalysisGroupIndex, WorkspaceInitializer workspaceInitializer) {
        this.sessionFactory = sessionFactory;
        this.devAnalysisGroupIndex = parseAnalysisGroupIndex(devAnalysisGroupIndex, "development");
        this.favoritesAnalysisGroupIndex = parseAnalysisGroupIndex(favoritesAnalysisGroupIndex, "favorites");
        this.workspaceInitializer = workspaceInitializer;
    }

    /**
     * Parses the development analysis group index, throwing an exception of the index is not a valid integer.
     *
     * @param devAnalysisGroupIndex the development analysis group index as a string.
     * @param description the analysis description.
     * @return the development analysis group index as an integer.
     */
    private int parseAnalysisGroupIndex(String devAnalysisGroupIndex, String description) {
        try {
            return Integer.parseInt(devAnalysisGroupIndex);
        }
        catch (NumberFormatException e) {
            String msg = "invalid " + description + " analysis group index: " + devAnalysisGroupIndex;
            throw new WorkflowException(msg, e);
        }
    }

    /**
     * Creates the object used to import all workflow elements.
     *
     * @param registry the object registry
     * @param session the hibernate session
     * @param updateMode indicates what should happen when an existing object matches one being imported.
     * @param updateVetted true if we should allow vetted analyses to be updated.
     * @return an instance of workflow importer.
     */
    private WorkflowImporter createWorkflowImporter(HeterogeneousRegistry registry, Session session,
            UpdateMode updateMode, boolean updateVetted) {
        WorkflowImporter importer = new WorkflowImporter();
        importer.addImporter("components", createDeployedComponentImporter(session, registry));
        importer.addImporter("templates", createTemplateImporter(session, registry, updateVetted));
        importer.addImporter("analyses", createAnalysisImporter(session, registry, updateVetted));
        importer.addImporter("notification_sets", createNotificationSetImporter(session, registry));
        importer.setUpdateMode(updateMode);
        return importer;
    }

    /**
     * Creates the object used to import template groups.
     *
     * @param daoFactory used to obtain data access objects.
     * @return the template group importer.
     */
    private TemplateGroupImporter createTemplateGroupImporter(DaoFactory daoFactory) {
        return new TemplateGroupImporter(daoFactory, devAnalysisGroupIndex, favoritesAnalysisGroupIndex);
    }

    /**
     * Creates the object used to import analyses (that is, transformation activities).
     *
     * @param session the database session.
     * @param registry the registry of named objects.
     * @param updateVetted true if we should allow vetted analyses to be updated.
     * @return the analysis importer.
     */
    private AnalysisImporter createAnalysisImporter(Session session, HeterogeneousRegistry registry,
            boolean updateVetted) {
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        TemplateGroupImporter templateGroupImporter = createTemplateGroupImporter(daoFactory);
        AnalysisImporter analysisImporter =
                new AnalysisImporter(daoFactory, templateGroupImporter, workspaceInitializer, updateVetted);
        analysisImporter.setRegistry(registry);
        analysisImporter.setSession(session);
        return analysisImporter;
    }

    /**
     * Creates the object used to import templates.
     *
     * @param session the database session.
     * @param registry the registry of named workflow elements.
     * @param updateVetted true if we should allow vetted analyses to be updated.
     * @return the template importer.
     */
    private TemplateImporter createTemplateImporter(Session session, HeterogeneousRegistry registry,
            boolean updateVetted) {
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        TemplateImporter templateImporter = new TemplateImporter(daoFactory, updateVetted, templateValidator);
        templateImporter.setRegistry(registry);
        return templateImporter;
    }

    /**
     * Creates the object used to import and automatically generate analyses for templates.
     *
     * @param session the database session.
     * @param registry the registry of named workflow elements.
     * @return the template importer.
     */
    private AnalysisGeneratingTemplateImporter createAnalysisGeneratingTemplateImporter(Session session,
            HeterogeneousRegistry registry) {
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        TemplateGroupImporter templateGroupImporter = createTemplateGroupImporter(daoFactory);
        AnalysisGeneratingTemplateImporter templateImporter = new AnalysisGeneratingTemplateImporter(daoFactory,
                templateGroupImporter, workspaceInitializer, templateValidator);
        templateImporter.setRegistry(registry);
        return templateImporter;
    }

    /**
     * Creates the object used to import notification sets.
     *
     * @param session the database session.
     * @param registry the registry of named workflow elements.
     * @return the notification set importer.
     */
    private NotificationSetImporter createNotificationSetImporter(Session session, HeterogeneousRegistry registry) {
        NotificationSetDao notificationSetDao = new HibernateNotificationSetDao(session);
        NotificationSetImporter notificationSetImporter = new NotificationSetImporter(notificationSetDao);
        notificationSetImporter.setRegistry(registry);
        return notificationSetImporter;
    }

    /**
     * Creates the object used to import deployed components.
     *
     * @param session the database session.
     * @param registry the registry of named workflow elements.
     * @return the deployed component importer.
     */
    private DeployedComponentImporter createDeployedComponentImporter(Session session, HeterogeneousRegistry registry) {
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        DeployedComponentImporter deployedComponentImporter = new DeployedComponentImporter(daoFactory);
        deployedComponentImporter.setRegistry(registry);
        return deployedComponentImporter;
    }

    /**
     * Imports a workflow.
     *
     * @param jsonString the string representing the JSON object to import.
     * @throws JSONException if the JSON string is invalid or doesn't meet the expectations of the workflow importer.
     */
    public String importWorkflow(String jsonString) throws JSONException {
        return importOrUpdateWorkflow(jsonString, UpdateMode.THROW, false);
    }

    /**
     * Updates a workflow.
     *
     * @param jsonString the string representing the JSON object to update.
     * @throws JSONException if the JSON string is invalid or doesn't meet the expectations of the workflow importer.
     */
    public String updateWorkflow(String jsonString) throws JSONException {
        return importOrUpdateWorkflow(jsonString, UpdateMode.REPLACE, false);
    }

    /**
     * Forces the update of a workflow.
     *
     * @param jsonString the string representing the JSON object to update.
     * @param updateModeName the name of the update mode to use.
     * @throws JSONException if the JSON string is invalid or doesn't meet the expectations of the workflow importer.
     */
    public String forceUpdateWorkflow(String jsonString, String updateModeName) throws JSONException {
        return importOrUpdateWorkflow(jsonString, getUpdateMode(updateModeName), true);
    }

    /**
     * Determines the update mode for the provided update mode name.
     *
     * @param updateModeName the update mode name.
     * @return the update mode.
     * @throws WorkflowException if the update mode isn't recognized.
     */
    private UpdateMode getUpdateMode(String updateModeName) {
        if (StringUtils.isBlank(updateModeName)) {
            return UpdateMode.DEFAULT;
        }
        else {
            try {
                return UpdateMode.valueOf(updateModeName.toUpperCase());
            }
            catch (IllegalArgumentException ignore) {
                throw new UnknownUpdateModeException(updateModeName);
            }
        }
    }

    /**
     * Either imports or updates a workflow. These two operations are similar enough to share a single method.
     *
     * @param jsonString the string representing the JSON object to import.
     * @param updateMode indicates what should happen when an existing object matches one being imported.
     * @param updateVetted true if we should allow vetted analyses to be updated.
     */
    private String importOrUpdateWorkflow(final String jsonString, final UpdateMode updateMode,
            final boolean updateVetted) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return importOrUpdateWorkflow(session, jsonString, updateMode, updateVetted);
            }
        });
    }

    /**
     * Either imports or updates a workflow.
     * 
     * @param session the Hibernate session.
     * @param jsonString the string representing the JSON object to import.
     * @param updateMode indicates what should happen when an existing object matches one being imported.
     * @param updateVetted true if we should allow vetted analyses and templates to be updated.
     * @return
     */
    private String importOrUpdateWorkflow(Session session, String jsonString, UpdateMode updateMode,
            boolean updateVetted) {
        try {
            HeterogeneousRegistry registry = new HeterogeneousRegistryImpl();
            JSONObject json = new JSONObject(jsonString);
            WorkflowImporter importer = createWorkflowImporter(registry, session, updateMode, updateVetted);
            return importer.importWorkflow(json);
        }
        catch (JSONException e) {
            throw new WorkflowException(e);
        }
        catch (HibernateException e) {
            logHibernateExceptionCause(e);
            throw e;
        }
    }

    /**
     * Imports a template, generating a single-step analysis for it.
     *
     * @param jsonString the string representing the JSON object to import.
     * @return the ID of the imported template.
     * @throws JSONException if the JSON string is invalid or doesn't meet the expectations of the importer.
     */
    public String importTemplate(String jsonString) throws JSONException {
        return importOrUpdateTemplate(jsonString, false);
    }

    /**
     * Updates a template.
     *
     * @param jsonString the string representing the template to update.
     * @return the ID of the imported template.
     * @throws JSONException if the JSON string is invalid or doesn't meet the expectations of the importer.
     */
    public String updateTemplate(String jsonString) throws JSONException {
        return importOrUpdateTemplate(jsonString, true);
    }

    /**
     * Either imports or updates a template.
     *
     * @param jsonString the string representing the template to import or update.
     * @param update true if existing analyses should be updated.
     * @return the ID of the imported template.
     */
    private String importOrUpdateTemplate(final String jsonString, final boolean update) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return importOrUpdateTemplate(session, jsonString, update);
            }
        });
    }

    /**
     * Either imports or updates a template.
     *
     * @param session the Hibernate session.
     * @param jsonString the string representing the template to import or update.
     * @param update true if existing analyses should be updated.
     * @return the ID of the imported template.
     */
    private String importOrUpdateTemplate(Session session, String jsonString, boolean update) {
        try {
            HeterogeneousRegistry registry = new HeterogeneousRegistryImpl();
            JSONObject json = new JSONObject(jsonString);
            AnalysisGeneratingTemplateImporter importer = createAnalysisGeneratingTemplateImporter(session, registry);
            if (update) {
                importer.enableReplacement();
            }
            String result = importer.importObject(json);
            return result;
        }
        catch (JSONException e) {
            throw new WorkflowException(e);
        }
        catch (HibernateException e) {
            logHibernateExceptionCause(e);
            return null;
        }
    }

    /**
     * Provides a way to update only the fields in an analysis (transformation activity) without updating any of the
     * components of the analysis.
     *
     * @param jsonString a JSON object containing information from the fields to update.
     */
    public void updateAnalysisOnly(final String jsonString) {
        new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<Void>() {
        @Override
            public Void perform(Session session) {
                new AnalysisUpdater(new HibernateDaoFactory(session)).updateAnalysis(jsonString);
                return null;
            }
        });
    }

    /**
     * Logs the cause of a Hibernate exception.
     *
     * @param e the exception.
     */
    private void logHibernateExceptionCause(HibernateException e) throws WorkflowException {
        Throwable currentException = e.getCause();
        Throwable nextException = null;
        while (currentException != null) {
            if (currentException instanceof SQLException) {
                SQLException sqlException = (SQLException) currentException;
                if (sqlException.getNextException() != null) {
                    LOG.error("Next Exception: ", sqlException.getNextException());
                    if (nextException == null) {
                        nextException = sqlException.getNextException();
                    }
                }
            }
            currentException = currentException.getCause();
        }
        if (nextException != null) {
            throw new WorkflowException(nextException.getMessage());
        }
        else {
            throw e;
        }
    }

    /**
     * @param templateValidator the new template validator.
     */
    public void setTemplateValidator(TemplateValidator templateValidator) {
        this.templateValidator = templateValidator;
    }
}
