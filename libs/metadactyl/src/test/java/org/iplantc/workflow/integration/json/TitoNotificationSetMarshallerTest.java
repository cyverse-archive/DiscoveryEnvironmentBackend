package org.iplantc.workflow.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.json.TitoNotificationSetMarshaller.
 * 
 * @author Dennis Roberts
 */
public class TitoNotificationSetMarshallerTest {

    /**
     * The marshaller to use in each of the unit tests.
     */
    private TitoNotificationSetMarshaller marshaller;

    /**
     * Used to obtain data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * Initializes each unit tests.
     */
    @Before
    public void initialize() {
        initializeDaoFactory();
        marshaller = new TitoNotificationSetMarshaller();
    }

    /**
     * Initializes the DAO factory.
     */
    private void initializeDaoFactory() {
        daoFactory = new MockDaoFactory();
        daoFactory.getTransformationActivityDao().save(UnitTestUtils.createAnalysis("analysis"));
    }

    /**
     * Verifies that the marshaller correctly marshals the fields in the notification set itself.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalNotificationSetFields() throws JSONException {
        JSONObject notificationSet = marshaller.toJson(createNotificationSet());
        assertEquals("notificationsetid", notificationSet.getString("id"));
        assertEquals("notificationsetname", notificationSet.getString("name"));
        assertEquals("analysisid", notificationSet.getString("analysis_id"));
    }

    /**
     * Verifies that the marshaller correctly marshals the fields in the notifications.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalNotifications() throws JSONException {
        JSONObject notificationSet = marshaller.toJson(createNotificationSet());
        assertTrue(notificationSet.has("wizard_notifications"));

        JSONArray notifications = notificationSet.getJSONArray("wizard_notifications");
        assertEquals(2, notifications.length());

        JSONObject notification1 = notifications.getJSONObject(0);
        assertEquals("firstnotification_sender", notification1.getString("sender"));
        assertEquals("updateData", notification1.getString("type"));
        assertTrue(notification1.has("receivers"));

        JSONArray receivers1 = notification1.getJSONArray("receivers");
        assertEquals(3, receivers1.length());
        assertEquals("firstnotification_firstreceiver", receivers1.getString(0));
        assertEquals("firstnotification_secondreceiver", receivers1.getString(1));
        assertEquals("firstnotification_thirdreceiver", receivers1.getString(2));

        JSONObject notification2 = notifications.getJSONObject(1);
        assertEquals("secondnotification_sender", notification2.getString("sender"));
        assertEquals("doSomething", notification2.getString("type"));
        assertTrue(notification2.has("receivers"));

        JSONArray receivers2 = notification2.getJSONArray("receivers");
        assertEquals(1, receivers2.length());
        assertEquals("secondnotification_firstreceiver", receivers2.getString(0));
    }

    /**
     * Verifies that the marshaller uses backward references if we tell it to.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldUseBackwardReferences() throws JSONException {
        marshaller = new TitoNotificationSetMarshaller(daoFactory, true);
        JSONObject notificationSet = marshaller.toJson(createNotificationSet());
        assertFalse(notificationSet.has("analysis_id"));
        assertEquals("analysis", notificationSet.getString("analysis_ref"));
    }

    /**
     * Verifies that the marshaller throws an exception if it tries to find the name of a referenced analysis that does
     * not exist.
     */
    @Test(expected = WorkflowException.class)
    public void shouldThrowExceptionForUnknownAnalysisWhenReferencesAreEnabled() {
        marshaller = new TitoNotificationSetMarshaller(daoFactory, true);
        NotificationSet notificationSet = createNotificationSet();
        notificationSet.setTemplate_id("unknown");
        marshaller.toJson(notificationSet);
    }

    /**
     * Verifies that the marshaller does not throw an exception for an unknown analysis when references are disabled.
     */
    @Test
    public void shouildNotThrowExceptionForUnknownAnalysisWhenReferencesAreDisabled() {
        NotificationSet notificationSet = createNotificationSet();
        notificationSet.setTemplate_id("unknown");
        marshaller.toJson(notificationSet);
    }

    /**
     * Creates the notification set to use for testing.
     * 
     * @return the notification set.
     */
    private NotificationSet createNotificationSet() {
        NotificationSet notificationSet = new NotificationSet();
        notificationSet.setIdc("notificationsetid");
        notificationSet.setName("notificationsetname");
        notificationSet.setTemplate_id("analysisid");
        notificationSet.addNotification(createFirstNotification());
        notificationSet.addNotification(createSecondNotification());
        return notificationSet;
    }

    /**
     * Creates the first notification in the notification set.
     * 
     * @return the notification.
     */
    private Notification createFirstNotification() {
        Notification notification = new Notification();
        notification.setSender("firstnotification_sender");
        notification.setType("updateData");
        notification.addreceiver("firstnotification_firstreceiver");
        notification.addreceiver("firstnotification_secondreceiver");
        notification.addreceiver("firstnotification_thirdreceiver");
        return notification;
    }

    /**
     * Creates the second notification in the notification set.
     * 
     * @return the notification.
     */
    private Notification createSecondNotification() {
        Notification notification = new Notification();
        notification.setSender("secondnotification_sender");
        notification.setType("doSomething");
        notification.addreceiver("secondnotification_firstreceiver");
        return notification;
    }
}
