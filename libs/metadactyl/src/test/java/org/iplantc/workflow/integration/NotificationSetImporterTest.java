package org.iplantc.workflow.integration;

import org.iplantc.workflow.WorkflowException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.iplantc.workflow.dao.mock.MockNotificationSetDao;
import org.iplantc.workflow.integration.util.HeterogeneousRegistryImpl;
import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.NotificationSetImporter.
 *
 * @author Dennis Roberts
 */
public class NotificationSetImporterTest {

    /**
     * Used to save imported notification sets.
     */
    private MockNotificationSetDao notificationSetDao;

    /**
     * The notification set importer instance that is being tested.
     */
    private NotificationSetImporter importer;

    /**
     * Initializes each of the tests.
     */
    @Before
    public void initialize() {
        notificationSetDao = new MockNotificationSetDao();
        importer = new NotificationSetImporter(notificationSetDao);
    }

    /**
     * Verifies that we can import a fully specified notification set.
     *
     * @throws JSONException if the JSON object we're importing is invalid.
     */
    @Test
    public void testFullySpecifiedNotificationSet() throws JSONException {
        String jsonString = "{   \"id\": \"someid\",\n"
            + "    \"name\": \"name\",\n"
            + "    \"analysis_id\": \"someanalysisid\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"id\": \"someotherid\",\n"
            + "            \"name\": \"someothername\",\n"
            + "            \"sender\": \"some_sender\",\n"
            + "            \"type\": \"sometype\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "                \"glarb_quux\"\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, notificationSetDao.getSavedObjects().size());
        NotificationSet notificationSet = notificationSetDao.getSavedObjects().get(0);
        assertEquals("someid", notificationSet.getIdc());
        assertEquals("name", notificationSet.getName());
        assertEquals("someanalysisid", notificationSet.getTemplate_id());
        assertEquals(1, notificationSet.getNotifications().size());
        Notification notification = notificationSet.getNotifications().get(0);
        assertEquals("someotherid", notification.getIdc());
        assertEquals("someothername", notification.getName());
        assertEquals("sometype", notification.getType());
        assertEquals(2, notification.getReceivers().size());
        assertEquals("baz_blrfl", notification.getReceivers().get(0));
        assertEquals("glarb_quux", notification.getReceivers().get(1));
    }

    /**
     * Verifies that we can import a minimally specified notification set.
     *
     * @throws JSONException if the JSON object we're importing is invalid.
     */
    @Test
    public void testMinimallySpecifiedNotificationSet() throws JSONException {
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_id\": \"someanalysisid\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"type\": \"sometype\",\n"
            + "            \"sender\": \"some_sender\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\"\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
        assertEquals(1, notificationSetDao.getSavedObjects().size());
        NotificationSet notificationSet = notificationSetDao.getSavedObjects().get(0);
        assertTrue(notificationSet.getIdc().matches("[-0-9A-F]{36}"));
        assertEquals("name", notificationSet.getName());
        assertEquals(1, notificationSet.getNotifications().size());
        Notification notification = notificationSet.getNotifications().get(0);
        assertTrue(notification.getIdc().matches("[-0-9A-F]{36}"));
        assertNull(notification.getName());
        assertEquals(1, notification.getReceivers().size());
        assertEquals("baz_blrfl", notification.getReceivers().get(0));
    }

    /**
     * Verifies that a missing name generates an exception.
     *
     * @throws JSONException if the JSON document that we pass in is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingNameShouldCauseException() throws JSONException {
        String jsonString = "{   \"analysis_id\": \"someanalysisid\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"sender\": \"some_sender\",\n"
            + "            \"type\": \"sometype\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing analysis ID generates an exception.
     *
     * @throws JSONException if the JSON document that we pass in is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingAnalysisIdShouldCauseException() throws JSONException {
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"sender\": \"some_sender\",\n"
            + "            \"type\": \"sometype\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing notification sender generates an exception.
     *
     * @throws JSONException if the JSON document that we pass in is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingSenderShouldCauseException() throws JSONException {
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_id\": \"someanalysisid\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"type\": \"sometype\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing notification type generates an exception.
     *
     * @throws JSONException if the JSON document we pass in is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingNotificationTypeShouldCauseException() throws JSONException {
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_id\": \"someanalysisid\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"sender\": \"some_sender\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that a missing receiver list generates an exception.
     *
     * @throws JSONException if the JSON document we pass in is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingReceiversShouldCauseException() throws JSONException {
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_id\": \"someanalysisid\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"sender\": \"some_sender\",\n"
            + "            \"type\": \"sometype\",\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        importer.importObject(json);
    }

    /**
     * Verifies that we can import multiple notification sets at once.
     *
     * @throws JSONException if the JSON array that we're importing is invalid.
     */
    @Test
    public void testMultipleNotifications() throws JSONException {
        String jsonString = "[   {   \"id\": \"someid\",\n"
            + "        \"name\": \"name\",\n"
            + "        \"analysis_id\": \"someanalysisid\",\n"
            + "        \"wizard_notifications\": [\n"
            + "            {   \"id\": \"someotherid\",\n"
            + "                \"name\": \"someothername\",\n"
            + "                \"sender\": \"some_sender\",\n"
            + "                \"type\": \"sometype\",\n"
            + "                \"receivers\": [\n"
            + "                    \"baz_blrfl\",\n"
            + "                    \"glarb_quux\"\n"
            + "                ]\n"
            + "            }\n"
            + "        ]\n"
            + "    },\n"
            + "    {   \"name\": \"name\",\n"
            + "        \"analysis_id\": \"someotheranalysisid\",\n"
            + "        \"wizard_notifications\": [\n"
            + "            {   \"sender\": \"some_sender\",\n"
            + "                \"type\": \"sometype\",\n"
            + "                \"receivers\": [\n"
            + "                    \"baz_blrfl\"\n"
            + "                ]\n"
            + "            }\n"
            + "        ]\n"
            + "    }\n"
            + "]";
        JSONArray array = new JSONArray(jsonString);
        importer.importObjectList(array);
        assertEquals(2, notificationSetDao.getSavedObjects().size());
        NotificationSet notificationSet1 = notificationSetDao.getSavedObjects().get(0);
        assertEquals("someid", notificationSet1.getIdc());
        assertEquals("name", notificationSet1.getName());
        assertEquals("someanalysisid", notificationSet1.getTemplate_id());
        assertEquals(1, notificationSet1.getNotifications().size());
        Notification notification1 = notificationSet1.getNotifications().get(0);
        assertEquals("someotherid", notification1.getIdc());
        assertEquals("someothername", notification1.getName());
        assertEquals("sometype", notification1.getType());
        assertEquals(2, notification1.getReceivers().size());
        assertEquals("baz_blrfl", notification1.getReceivers().get(0));
        assertEquals("glarb_quux", notification1.getReceivers().get(1));
        NotificationSet notificationSet2 = notificationSetDao.getSavedObjects().get(1);
        assertTrue(notificationSet2.getIdc().matches("[-0-9A-F]{36}"));
        assertEquals("name", notificationSet2.getName());
        assertEquals(1, notificationSet2.getNotifications().size());
        Notification notification2 = notificationSet2.getNotifications().get(0);
        assertTrue(notification2.getIdc().matches("[-0-9A-F]{36}"));
        assertNull(notification2.getName());
        assertEquals(1, notification2.getReceivers().size());
        assertEquals("baz_blrfl", notification2.getReceivers().get(0));
    }

    /**
     * Verifies that the importer can handle a reference to a named analysis.
     *
     * @throws JSONException if the JSON object doesn't satisfy the importer's requirements.
     */
    @Test
    public void shouldReferenceNamedAnalysis() throws JSONException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_ref\": \"foo\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"type\": \"sometype\",\n"
            + "            \"sender\": \"some_sender\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\"\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        importer.importObject(new JSONObject(jsonString));
        assertEquals(1, notificationSetDao.getSavedObjects().size());
        NotificationSet notificationSet = notificationSetDao.getSavedObjects().get(0);
        assertEquals("fooid", notificationSet.getTemplate_id());
    }

    /**
     * Verifies that the importer can reference more than one named analysis.
     *
     * @throws JSONException if the JSON object doesn't satisfy the importer's requirements.
     */
    @Test
    public void shouldReferenceMultipleNamedAnalyses() throws JSONException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        String jsonString = "[   {   \"id\": \"someid\",\n"
            + "        \"name\": \"name\",\n"
            + "        \"analysis_ref\": \"foo\",\n"
            + "        \"wizard_notifications\": [\n"
            + "            {   \"id\": \"someotherid\",\n"
            + "                \"name\": \"someothername\",\n"
            + "                \"sender\": \"some_sender\",\n"
            + "                \"type\": \"sometype\",\n"
            + "                \"receivers\": [\n"
            + "                    \"baz_blrfl\",\n"
            + "                    \"glarb_quux\"\n"
            + "                ]\n"
            + "            }\n"
            + "        ]\n"
            + "    },\n"
            + "    {   \"name\": \"name\",\n"
            + "        \"analysis_ref\": \"bar\",\n"
            + "        \"wizard_notifications\": [\n"
            + "            {   \"sender\": \"some_sender\",\n"
            + "                \"type\": \"sometype\",\n"
            + "                \"receivers\": [\n"
            + "                    \"baz_blrfl\"\n"
            + "                ]\n"
            + "            }\n"
            + "        ]\n"
            + "    }\n"
            + "]";
        importer.importObjectList(new JSONArray(jsonString));
        assertEquals(2, notificationSetDao.getSavedObjects().size());
        assertEquals("fooid", notificationSetDao.getSavedObjects().get(0).getTemplate_id());
        assertEquals("barid", notificationSetDao.getSavedObjects().get(1).getTemplate_id());
    }

    /**
     * Verifies that an unknown analysis name causes an exception.
     *
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    @Test(expected = JSONException.class)
    public void unknownAnalysisNameShouldCauseException() throws JSONException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_ref\": \"oof\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"type\": \"sometype\",\n"
            + "            \"sender\": \"some_sender\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        importer.importObject(new JSONObject(jsonString));
    }

    /**
     * Verifies that JSON object with no analysis name or ID causes an exception.
     *
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    @Test(expected = JSONException.class)
    public void missingAnalysisRefAndIdShouldCauseException() throws JSONException {
        HeterogeneousRegistryImpl registry = UnitTestUtils.createRegistry();
        importer.setRegistry(registry);
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"type\": \"sometype\",\n"
            + "            \"sender\": \"some_sender\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\",\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        importer.importObject(new JSONObject(jsonString));
    }

    /**
     * Verifies that notification sets are replaced if replacement is enabled.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldReplaceNotificationSetsIfReplacementEnabled() throws JSONException {
        importer.enableReplacement();
        importer.importObject(getMinimalNotificationSetJson("analysisid", "sometype"));
        importer.importObject(getMinimalNotificationSetJson("analysisid", "someothertype"));
        assertEquals(1, notificationSetDao.getSavedObjects().size());
        assertEquals("someothertype", notificationSetDao.getSavedObjects().get(0).getNotifications().get(0).getType());
    }

    /**
     * Verifies that an exception is thrown if notification set replacement is disabled and someone attempts to replace
     * a notification set.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test(expected = WorkflowException.class)
    public void shouldThrowExceptionIfReplacementDisabled() throws JSONException {
        importer.disableReplacement();
        importer.importObject(getMinimalNotificationSetJson("analysisid", "sometype"));
        importer.importObject(getMinimalNotificationSetJson("analysisid", "someothertype"));
    }

    /**
     * Verifies that notification sets are not replaced if replacement is ignored.
     *
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldNotReplaceNotificationSetsIfReplacementIgnored() throws JSONException {
        importer.ignoreReplacement();
        importer.importObject(getMinimalNotificationSetJson("analysisid", "sometype"));
        importer.importObject(getMinimalNotificationSetJson("analysisid", "someothertype"));
        assertEquals(1, notificationSetDao.getSavedObjects().size());
        assertEquals("sometype", notificationSetDao.getSavedObjects().get(0).getNotifications().get(0).getType());
    }

    /**
     * Creates a minimal notification set JSON object for testing.
     *
     * @param analysisId the analysis identifier to use in the unit test.
     * @param type the type of notification to include in the notification set.
     * @return the JSON object.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject getMinimalNotificationSetJson(String analysisId, String type) throws JSONException {
        String jsonString = "{   \"name\": \"name\",\n"
            + "    \"analysis_id\": \"" + analysisId + "\",\n"
            + "    \"wizard_notifications\": [\n"
            + "        {   \"type\": \"" + type + "\",\n"
            + "            \"sender\": \"some_sender\",\n"
            + "            \"receivers\": [\n"
            + "                \"baz_blrfl\"\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}\n";
        return new JSONObject(jsonString);
    }
}
