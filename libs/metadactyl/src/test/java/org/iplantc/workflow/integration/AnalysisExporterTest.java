package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DeployedComponentDao;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.dao.TemplateDao;
import org.iplantc.workflow.dao.TransformationActivityDao;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.AnalysisExporter.
 *
 * @author Dennis Roberts
 */
public class AnalysisExporterTest {

    /**
     * Used to obtain data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The exporter being tested.
     */
    private AnalysisExporter exporter;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        initializeDaoFactory();
        exporter = new AnalysisExporter(daoFactory);
    }

    /**
     * Initializes the DAO factory for testing.
     */
    private void initializeDaoFactory() {
        daoFactory = new MockDaoFactory();
        saveDeployedComponents(daoFactory.getDeployedComponentDao());
        saveTemplates(daoFactory.getTemplateDao());
        saveAnalyses(daoFactory.getTransformationActivityDao());
        saveNotificationSets(daoFactory.getNotificationSetDao());
    }

    /**
     * Saves the test notification sets to the notification set DAO.
     *
     * @param notificationSetDao the notification set DAO.
     */
    private void saveNotificationSets(NotificationSetDao notificationSetDao) {
        notificationSetDao.save(createTestNotificationSet());
    }

    /**
     * Creates a notification set to use for testing.
     *
     * @return the notification set.
     */
    private NotificationSet createTestNotificationSet() {
        NotificationSet notificationSet = new NotificationSet();
        notificationSet.setIdc("notificationsetid");
        notificationSet.setName("notificationset");
        notificationSet.setTemplate_id("multistepid");
        notificationSet.addNotification(createTestNotification());
        return notificationSet;
    }

    /**
     * Creates a notification for testing.
     *
     * @return the notification.
     */
    private Notification createTestNotification() {
        Notification notification = new Notification();
        notification.setSender("transformation_step_for_template1id_foo");
        notification.addreceiver("transformation_step_for_template1id_bar");
        notification.addreceiver("transformation_step_for_template2id_baz");
        return notification;
    }

    /**
     * Saves the test analyses to the analysis DAO.
     *
     * @param analysisDao the analysis DAO.
     */
    private void saveAnalyses(TransformationActivityDao analysisDao) {
        analysisDao.save(UnitTestUtils.createAnalysisWithSteps("singlestep", "template1id"));
        analysisDao.save(UnitTestUtils.createAnalysisWithSteps("multistep", "template1id", "template2id"));
    }

    /**
     * Saves the test templates to the template DAO.
     *
     * @param templateDao the template DAO.
     */
    private void saveTemplates(TemplateDao templateDao) {
        templateDao.save(UnitTestUtils.createTemplate("template1", "component1id"));
        templateDao.save(UnitTestUtils.createTemplate("template2", "component2id"));
    }

    /**
     * Saves the test deployed components to the deployed component DAO.
     *
     * @param deployedComponentDao the deployed component DAO.
     */
    private void saveDeployedComponents(DeployedComponentDao deployedComponentDao) {
        deployedComponentDao.save(UnitTestUtils.createDeployedComponent("component1", "component1id"));
        deployedComponentDao.save(UnitTestUtils.createDeployedComponent("component2", "component2id"));
    }

    /**
     * Verifies that the exporter successfully exports a single step analysis.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldExportSingleStepAnalysis() throws JSONException {
        JSONObject json = exporter.exportAnalysis("singlestepid");
        assertTrue(json.has("components"));
        assertTrue(json.has("templates"));
        assertTrue(json.has("analyses"));
        assertFalse(json.has("notification_sets"));

        JSONArray components = json.getJSONArray("components");
        assertEquals(1, components.length());
        assertTrue(arrayContainsObjectWithFieldValue(components, "id", "component1id"));

        JSONArray templates = json.getJSONArray("templates");
        assertEquals(1, templates.length());
        assertTrue(arrayContainsObjectWithFieldValue(templates, "id", "template1id"));

        JSONArray analyses = json.getJSONArray("analyses");
        assertEquals(1, analyses.length());
        assertTrue(arrayContainsObjectWithFieldValue(analyses, "analysis_id", "singlestepid"));
    }

    /**
     * Verifies that the exporter successfully exports a multistep analysis.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldExportMultistepAnalysis() throws JSONException {
        JSONObject json = exporter.exportAnalysis("multistepid");
        assertTrue(json.has("components"));
        assertTrue(json.has("templates"));
        assertTrue(json.has("analyses"));
        assertTrue(json.has("notification_sets"));

        JSONArray components = json.getJSONArray("components");
        assertEquals(2, components.length());
        assertTrue(arrayContainsObjectWithFieldValue(components, "id", "component1id"));
        assertTrue(arrayContainsObjectWithFieldValue(components, "id", "component2id"));

        JSONArray templates = json.getJSONArray("templates");
        assertEquals(2, templates.length());
        assertTrue(arrayContainsObjectWithFieldValue(templates, "id", "template1id"));
        assertTrue(arrayContainsObjectWithFieldValue(templates, "id", "template2id"));

        JSONArray analyses = json.getJSONArray("analyses");
        assertEquals(1, analyses.length());
        assertTrue(arrayContainsObjectWithFieldValue(analyses, "analysis_id", "multistepid"));

        JSONArray notificationSets = json.getJSONArray("notification_sets");
        assertEquals(1, notificationSets.length());
        assertTrue(arrayContainsObjectWithFieldValue(notificationSets, "id", "notificationsetid"));
    }

    /**
     * Verifies that we get an exception for an unknown analysis identifier.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUnknownAnalysisId() {
        exporter.exportAnalysis("unknown");
    }

    /**
     * Verifies that we get an exception for an analysis that references an unknown template.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUnknownTemplate() {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById("singlestepid");
        analysis.getSteps().get(0).getTransformation().setTemplate_id("unknown");
        exporter.exportAnalysis("singlestepid");
    }

    /**
     * Verifies that we get an exception for a template that references an unknown deployed component.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUnknownDeployedComponent() {
        Template template = daoFactory.getTemplateDao().findById("template1id");
        template.setComponent("unknown");
        exporter.exportAnalysis("singlestepid");
    }

    /**
     * Verifies that the given JSON array contains a JSON object with a specific field value.
     *
     * @param array the JSON array.
     * @param key the key representing the field to check.
     * @param value the field value to search for.
     * @return true if a matching object is found.
     * @throws JSONException if a JSON error occurs.
     */
    private boolean arrayContainsObjectWithFieldValue(JSONArray array, String key, String value) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            if (StringUtils.equals(array.getJSONObject(i).optString(key), value)) {
                return true;
            }
        }
        return false;
    }
}
