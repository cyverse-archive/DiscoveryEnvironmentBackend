package org.iplantc.workflow.service;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.workflow.AnalysisNotFoundException;
import org.iplantc.workflow.AnalysisOwnershipException;
import org.iplantc.workflow.AnalysisStepCountException;
import org.iplantc.workflow.TemplateNotFoundException;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.integration.json.CopyIdRetentionStrategy;
import org.iplantc.workflow.integration.json.IdRetentionStrategy;
import org.iplantc.workflow.integration.json.NoIdRetentionStrategy;
import org.iplantc.workflow.integration.json.TitoIntegrationDatumMashaller;
import org.iplantc.workflow.integration.json.TitoTemplateMarshaller;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.service.dto.AnalysisId;
import org.iplantc.workflow.user.UserDetails;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A service that allows analyses to be exported to Tito for editing.
 *
 * @author Dennis Roberts
 */
public class AnalysisEditService {

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * Used to get the user's details.
     */
    private UserService userService;

    /**
     * Used to save apps.
     */
    private WorkflowImportService workflowImportService;

    /**
     * @param sessionFactory the Hibernate session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param userService used to get the user's details.
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * @param workflowImportService used to save apps.
     */
    public void setWorkflowImportService(WorkflowImportService workflowImportService) {
        this.workflowImportService = workflowImportService;
    }

    /**
     * Prepares an analysis for editing. If the analysis already belongs to the user in TITO then this service merely
     * ensures that the analysis is not marked as deleted. If the analysis does not belong to the user in TITO, this
     * service makes a copy of the analysis for the user in TITO and returns the new analysis ID.
     *
     * @param analysisId the analysis identifier.
     * @return the (possibly new) analysis identifier.
     */
    public String prepareAnalysisForEditing(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return editAnalysis(new HibernateDaoFactory(session), analysisId);
            }
        });
    }

    /**
     * Copies an analysis. This is different from preparing an analysis for editing in that a new copy of the analysis
     * is created even if the user has the ability to edit the original.
     *
     * @param analysisId the original analysis identifier.
     * @return the new analysis identifier.
     */
    public String copyAnalysis(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return copyAnalysis(new HibernateDaoFactory(session), analysisId);
            }
        });
    }

    /**
     * Prepares an analysis for editing. If the analysis already belongs to the user in TITO then this service merely
     * ensures that the analysis is not marked as deleted. If the analysis does not belong to the user in TITO, this
     * service makes a copy of the analysis for the user in TITO and returns the new analysis ID.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the analysis identifier.
     * @return the (possibly new) analysis identifier.
     */
    private String editAnalysis(DaoFactory daoFactory, String analysisId) {
        TransformationActivity analysis = getAnalysis(daoFactory, analysisId);
        UserDetails userDetails = userService.getCurrentUserDetails();
        verifyUserOwnership(analysis, userDetails);
        verifyNumberOfSteps(analysis);
        return AddObjectWrapper(marshalAnalysis(daoFactory, analysis, new CopyIdRetentionStrategy())).toString();
    }

    /**
     * Adds a wrapper around an analysis similar to the one returned by the OSM.
     *
     * @param analysis the analysis to wrap.
     * @return the wrapped analysis.
     * @throws WorkflowException if a JSON error occurs.
     */
    private JSONObject AddObjectWrapper(JSONObject analysis) {
        try {
            JSONObject wrapper = new JSONObject();
            JSONArray objects = new JSONArray();
            objects.put(analysis);
            wrapper.put("objects", objects);
            return wrapper;
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to add object wrapper", e);
        }
    }

    /**
     * Finds an analysis in the database.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the analysis identifier.
     * @return the analysis;
     * @throws AnalysisNotFoundException if an analysis with the given ID can't be found.
     */
    private TransformationActivity getAnalysis(DaoFactory daoFactory, String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new AnalysisNotFoundException(analysisId);
        }
        return analysis;
    }

    /**
     * Obtains the first template associated with an analysis.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysis the analysis.
     * @return the template.
     * @throws templateNotFoundException if the template can't be found.
     */
    private Template getFirstTemplate(DaoFactory daoFactory, TransformationActivity analysis) {
        String templateId = analysis.step(0).getTemplateId();
        Template template = daoFactory.getTemplateDao().findById(templateId);
        if (template == null) {
            throw new TemplateNotFoundException(templateId);
        }
        return template;
    }

    /**
     * Verifies that the user owns the analysis that is being edited.
     *
     * @param analysis the analysis.
     * @param userDetails details about the authenticated user.
     * @throws AnalysisOwnershipException if the user does not own the analysis.
     */
    private void verifyUserOwnership(TransformationActivity analysis, UserDetails userDetails) {
        String integratorEmail = analysis.getIntegrationDatum().getIntegratorEmail();
        String authenticatedEmail = userDetails.getEmail();
        if (!StringUtils.equals(integratorEmail, authenticatedEmail)) {
            throw new AnalysisOwnershipException(userDetails.getShortUsername(), analysis.getId());
        }
    }

    /**
     * Verifies that Tito is capable of editing the analysis based on the number of steps.
     *
     * @param analysis the analysis to validate.
     */
    private void verifyNumberOfSteps(TransformationActivity analysis) {
        if (analysis.getStepCount() != 1) {
            throw new AnalysisStepCountException(analysis.getId(), analysis.getStepCount());
        }
    }

    /**
     * Marshals an analysis.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysis the analysis to marshal.
     * @return the marshaled analysis.
     */
    private JSONObject marshalAnalysis(DaoFactory daoFactory, TransformationActivity analysis,
            IdRetentionStrategy idRetentionStrategy) {
        Template template = getFirstTemplate(daoFactory, analysis);
        TitoTemplateMarshaller marshaller = new TitoTemplateMarshaller(daoFactory, false, idRetentionStrategy);
        return marshaller.toJson(template, analysis);
    }

    /**
     * Prepares a new copy of an analysis for editing. This is different from editAnalysis in that a new copy of the
     * analysis is made even if the user already has the ability to edit the original.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the original analysis identifier.
     * @return the new analysis identifier.
     */
    private String copyAnalysis(DaoFactory daoFactory, String analysisId) {
        return copyAnalysis(daoFactory, analysisId, userService.getCurrentUserDetails());
    }

    /**
     * Saves an app using the metadata import service.
     *
     * @param jsonString the JSON representation of the analysis.
     */
    private void importAnalysis(String jsonString) {
        try {
            workflowImportService.importTemplate(jsonString);
        }
        catch (JSONException e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * Prepares a new copy of an analysis for editing. This is different from editAnalysis in that a new copy of the
     * analysis is made even if the user already has the ability to edit the original.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the original analysis identifier.
     * @param userDetails information about the current user.
     * @return the new analysis identifier.
     */
    private String copyAnalysis(DaoFactory daoFactory, String analysisId, UserDetails userDetails) {
        TransformationActivity analysis = getAnalysis(daoFactory, analysisId);
        verifyNumberOfSteps(analysis);
        return copyAnalysis(daoFactory, analysis, userDetails);
    }

    /**
     * Prepares a new copy of an analysis for editing.
     *
     * @param daoFactory used to obtain data access objects.
     * @param analysis the analysis to copy.
     * @param userDetails information about the current user.
     * @return the new analysis identifier.
     */
    private String copyAnalysis(DaoFactory daoFactory, TransformationActivity analysis, UserDetails userDetails) {
        JSONObject json = marshalAnalysis(daoFactory, analysis, new NoIdRetentionStrategy());
        json.remove("published_date");
        json.remove("edited_date");
        String newId = UUID.randomUUID().toString().toUpperCase();
        json = convertAnalysisToCopy(json, newId, userDetails);
        importAnalysis(json.toString());
        return new AnalysisId(newId).toString();
    }

    /**
     * Converts the app to a copy for the given analysis identifier and user info.
     *
     * @param analysis the app to convert.
     * @param userDetails information about the current user.
     * @return the app as a copy.
     */
    private JSONObject convertAnalysisToCopy(JSONObject analysis, String newId, UserDetails userDetails) {
        TitoIntegrationDatumMashaller marshaller = new TitoIntegrationDatumMashaller();
        try {
            analysis.put("id", newId);
            analysis.put("tito", newId);
            analysis.put("name", "Copy of " + analysis.getString("name"));
            analysis.put("implementation", marshaller.toJson(createIntegrationDatum(userDetails)));
            analysis.put("user", userDetails.getShortUsername());
            analysis.put("full_username", userDetails.getUsername());
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to convert analysis", e);
        }
        return analysis;
    }

    /**
     * Creates an integration datum for the current user.
     *
     * @param userDetails information about the authenticated user.
     * @return the integration datum.
     */
    private IntegrationDatum createIntegrationDatum(UserDetails userDetails) {
        IntegrationDatum integrationDatum = new IntegrationDatum();
        integrationDatum.setIntegratorEmail(userDetails.getEmail());
        integrationDatum.setIntegratorName(userDetails.getFirstName() + " " + userDetails.getLastName());
        return integrationDatum;
    }
}
