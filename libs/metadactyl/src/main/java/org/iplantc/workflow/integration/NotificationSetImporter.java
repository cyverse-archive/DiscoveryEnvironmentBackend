package org.iplantc.workflow.integration;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.integration.json.TitoNotificationSetUnmarshaller;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to import notification sets from JSON objects. Each JSON object must contain a name, template identifier and
 * a list of wizard notifications. An id field may also be specified; if the identifier is left unspecified then an
 * identifier will be generated. The list of wizard notifications is a list of JSON objects. Each wizard
 * notification must contain a sender name, a notification type and a list of receiver names. The sender and receiver
 * names are always in the format &lt;step_name&gt;_&lt;property_name&gt;. This format allows the UI to associate
 * each event with the control that generates it and the set of controls that are affected by it. The format of the
 * JSON object is:
 *
 * <pre>
 * <code>
 * {   "id": &lt;notification_set_id&gt;,
 *     "name": &lt;notification_set_name&gt;,
 *     "analysis_id": &lt;analysis_id&gt;,
 *     "wizard_notifications": [
 *         {   "sender": &lt;sender_name&gt;,
 *             "type": &lt;notification_type&gt;,
 *             "receivers": [
 *                 &lt;receiver_name_1&gt;,
 *                 &lt;receiver_name_2&gt;,
 *                 ...,
 *                 &lt;receiver_name_n&gt;
 *             ]
 *         },
 *         ...
 *     ]
 * }
 * </code>
 * </pre>
 *
 * @author Dennis Roberts
 */
public class NotificationSetImporter implements ObjectImporter {

    /**
     * Used to save the notification sets.
     */
    private NotificationSetDao notificationSetDao;

    /**
     * The registry of named workflow elements.
     */
    private HeterogeneousRegistry registry = new NullHeterogeneousRegistry();

    /**
     * Indicates what should be done if an existing notification set matches the one that's being imported.
     */
    private UpdateMode updateMode = UpdateMode.DEFAULT;

    /**
     * Initializes a new instance of this class.
     *
     * @param notificationSetDao the object used to save the notification sets.
     */
    public NotificationSetImporter(NotificationSetDao notificationSetDao) {
        this.notificationSetDao = notificationSetDao;
    }

    /**
     * @param registry the new registry of named workflow elements.
     */
    public void setRegistry(HeterogeneousRegistry registry) {
        this.registry = registry == null ? new NullHeterogeneousRegistry() : registry;
    }

    /**
     * Enables the replacement of existing notification sets.
     */
    @Override
    public void enableReplacement() {
        setUpdateMode(UpdateMode.REPLACE);
    }

    /**
     * Disables the replacement of existing notification sets.
     */
    @Override
    public void disableReplacement() {
        setUpdateMode(UpdateMode.THROW);
    }

    /**
     * Instructs the importer to ignore attempts to replace existing notification sets.
     */
    @Override
    public void ignoreReplacement() {
        setUpdateMode(UpdateMode.IGNORE);
    }

    /**
     * Explicitly sets the update mode.
     *
     * @param updateMode the new update mode.
     */
    @Override
    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    /**
     * Imports a notification set using the values from the given JSON object.
     *
     * @param json the JSON object.
     * @return the notification set ID.
     * @throws JSONException if the JSON object is missing a required attribute or contains an unexpected type.
     */
    @Override
    public String importObject(JSONObject json) throws JSONException {
        TitoNotificationSetUnmarshaller unmarshaller = new TitoNotificationSetUnmarshaller(registry);
        NotificationSet notificationSet = unmarshaller.fromJson(json);
        List<NotificationSet> existingNotificationSets = findExistingNotificationSets(notificationSet);
        if (existingNotificationSets.isEmpty()) {
            notificationSetDao.save(notificationSet);
        }
        else if (updateMode == UpdateMode.REPLACE) {
            notificationSetDao.deleteAll(existingNotificationSets);
            notificationSetDao.save(notificationSet);
        }
        else if (updateMode == UpdateMode.THROW) {
            throw new WorkflowException("a duplicate notification set was found and replacement isn't enabled");
        }
        return notificationSet.getId();
    }

    /**
     * Finds the list of notification sets that are associated with the same analysis as the given notification set.
     *
     * @param notificationSet the new notification set.
     * @return the list of notification sets associated with the same analysis.
     */
    private List<NotificationSet> findExistingNotificationSets(NotificationSet notificationSet) {
        return notificationSetDao.findNotificationSetsForAnalysisId(notificationSet.getTemplate_id());
    }

    /**
     * Imports a list of notification sets using the values from the given JSON array.
     *
     * @param array the JSON array.
     * @return the list of notification set IDs.
     * @throws JSONException if any object in the JSON array is missing a required attribute or contains an unexpected
     *         type.
     */
    @Override
    public List<String> importObjectList(JSONArray array) throws JSONException {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++) {
            importObject(array.getJSONObject(i));
        }
        return result;
    }
}
