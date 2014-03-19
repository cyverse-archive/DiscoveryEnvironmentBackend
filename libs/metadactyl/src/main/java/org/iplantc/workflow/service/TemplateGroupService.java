package org.iplantc.workflow.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.authn.service.UserSessionService;
import org.iplantc.authn.user.User;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.persistence.dao.data.IntegrationDatumDao;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.workflow.AnalysisPublicException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.core.TransformationActivityReference;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.TemplateGroupDao;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.integration.validation.TemplateValidator;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.workflow.user.UserInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class TemplateGroupService {
    public static final String BETA_TEMPLATE_GROUP_ID = "g5401bd146c144470aedd57b47ea1b979";
    public static final String ANALYSIS_ID_KEY = "analysis_id";

    private SessionFactory sessionFactory;
    private UserSessionService userSessionService;
    private TemplateValidator templateValidator;

    public TemplateGroupService() {

    }

    /**
     * Helper function used to easily get a template group by id.  Throws a runtime
     * exception if the id isn't found.
     */
    private TemplateGroup getTemplateGroup(DaoFactory daoFactory, String templateGroupId) {
        TemplateGroup group = daoFactory.getTemplateGroupDao().findById(templateGroupId);

        if(group == null) {
            throw new RuntimeException("No group found with id " + templateGroupId);
        }

        return group;
    }

    /**
     * Helper function to get a transformation activity by id.  Throws a runtime
     * exception if the id isn't found.
     */
    private TransformationActivity getTransformationActivity(DaoFactory daoFactory, String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);

        if(analysis == null) {
            throw new RuntimeException("No analysis found with id " + analysisId);
        }

        return analysis;
    }

    public String makeAnalysisPublic(String jsonInput) throws Exception {
        final JSONObject input = new JSONObject(jsonInput);

        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                try {
                    String analysisId;
                    DaoFactory daoFactory = new HibernateDaoFactory(session);

                    analysisId = input.getString(ANALYSIS_ID_KEY);

                    TemplateGroupDao templateGroupDao = daoFactory.getTemplateGroupDao();

                    TemplateGroup group = templateGroupDao.findById(BETA_TEMPLATE_GROUP_ID);
                    TransformationActivity transformationActivity = getTransformationActivity(daoFactory, analysisId);
                    validateAnalysis(daoFactory, analysisId);
                    validateTemplates(daoFactory, transformationActivity);

                    fillIntegrationDatum(daoFactory, transformationActivity);
                    fillReferences(transformationActivity);
                    fillSuggestedGroups(daoFactory, transformationActivity);
                    transformationActivity.setDescription(input.optString("desc", transformationActivity.getDescription()));
                    transformationActivity.setName(input.optString("name", transformationActivity.getName()));
                    transformationActivity.setWikiurl(input.getString("wiki_url"));

                    if (transformationActivity.getIntegrationDate() == null) {
                        transformationActivity.setIntegrationDate(new Date());
                    }

                    // Remove Analysis from it's current groups
                    List<TemplateGroup> currentGroups = templateGroupDao.findTemplateGroupsContainingAnalysis(transformationActivity);
                    for (TemplateGroup templateGroup : currentGroups) {
                        templateGroup.removeTemplate(transformationActivity);
                    }

                    // Add the analysis to the beta group
                    group.addTemplate(transformationActivity);
                    templateGroupDao.save(group);

                    return "{}";
                } catch(JSONException jsonException) {
                    throw new RuntimeException(jsonException);
                }
            }

            private void validateAnalysis(DaoFactory factory, String analysisId) {
                AnalysisListing listing = factory.getAnalysisListingDao().findByExternalId(analysisId);
                if (listing == null) {
                    throw new RuntimeException("unable to find the listing for analysis, " + analysisId);
                }
                if (listing.isPublic()) {
                    throw new IllegalArgumentException("analysis, " + analysisId + ", is already public");
                }
            }

            private void validateTemplates(DaoFactory daoFactory, TransformationActivity analysis) {
                if (templateValidator != null) {
                    for (Template template : daoFactory.getTemplateDao().findTemplatesInAnalysis(analysis)) {
                        templateValidator.validate(template, null);
                    }
                }
            }

            private void fillIntegrationDatum(DaoFactory daoFactory, TransformationActivity transformationActivity) throws JSONException {
                // Fill in the transformation activity information
            	IntegrationDatumDao integrationDatumDao = daoFactory.getIntegrationDatumDao();
                User user = userSessionService.getUser();
            	String email = user.getEmail();
            	String integrator = user.getFirstName() + " " + user.getLastName();
                IntegrationDatum integrationDatum = integrationDatumDao.findByNameAndEmail(integrator, email);

                if(integrationDatum == null){
	                integrationDatum = new IntegrationDatum();
	                integrationDatum.setIntegratorEmail(email);
	                integrationDatum.setIntegratorName(integrator);
                }
                transformationActivity.setIntegrationDatum(integrationDatum);
            }

            private void fillReferences(TransformationActivity transformationActivity) throws JSONException {
                JSONArray references = input.getJSONArray("references");
                for (int i = 0; i < references.length(); i++) {
                    TransformationActivityReference ref = new TransformationActivityReference();
                    ref.setReferenceText(references.getString(i));
                    transformationActivity.getReferences().add(ref);
                }
            }

            private void fillSuggestedGroups(DaoFactory daoFactory, TransformationActivity transformationActivity) throws JSONException {
                TemplateGroupDao templateGroupDao = daoFactory.getTemplateGroupDao();
                JSONArray groups = input.getJSONArray("groups");

                for (int i = 0; i < groups.length(); i++) {
                    String id = groups.getString(i);
                    TemplateGroup templateGroup = templateGroupDao.findById(id);
                    if (templateGroup == null) {
                        throw new IllegalArgumentException("suggested app group, " + id + ", not found");
                    }
                    transformationActivity.getSuggestedGroups().add(templateGroup);
                }
            }
        });
    }

    /**
     * Service endpoint to add an Analysis to a Template Group.
     */
    public String addAnalysisToTemplateGroup(String jsonInput) throws Exception {
        final JSONObject input = new JSONObject(jsonInput);

        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                List<String> templateGroups = null;
                String analysisId;
                DaoFactory daoFactory = new HibernateDaoFactory(session);

                try {
                    analysisId = input.getString(ANALYSIS_ID_KEY);
                    templateGroups = extractTemplateGroupsFromJson();
                } catch(JSONException jsonException) {
                    throw new RuntimeException(jsonException);
                }

                if(templateGroups == null || templateGroups.isEmpty()) {
                    throw new RuntimeException("No groups provided in input.");
                } else if(analysisId == null) {
                    throw new RuntimeException("No analysis_id provided in input.");
                } else {
                    for (String groupId : templateGroups) {
                        TemplateGroup group = getTemplateGroup(daoFactory, groupId);
                        TransformationActivity analysis = getTransformationActivity(daoFactory, analysisId);

                        group.addTemplate(analysis);
                        daoFactory.getTemplateGroupDao().save(group);
                    }

                    return "{}";
                }
            }

            private List<String> extractTemplateGroupsFromJson() throws JSONException {
                List<String> templateGroups = new LinkedList<String>();
                JSONArray templateGroupsArray = input.getJSONArray("groups");

                for (int i = 0; i < templateGroupsArray.length(); i++) {
                    templateGroups.add(templateGroupsArray.getString(i));
                }

                return templateGroups;
            }
        });
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public UserSessionService getUserSessionService() {
        return userSessionService;
    }

    public void setUserSessionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    public void setTemplateValidator(TemplateValidator templateValidator) {
        this.templateValidator = templateValidator;
    }
}
