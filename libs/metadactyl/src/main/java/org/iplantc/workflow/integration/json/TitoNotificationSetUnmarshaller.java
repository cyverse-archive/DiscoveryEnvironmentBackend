package org.iplantc.workflow.integration.json;

import java.util.LinkedList;
import java.util.List;

import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Converts JSON documents representing notification sets to notification sets.
 *
 * @author Dennis Roberts
 */
public class TitoNotificationSetUnmarshaller implements TitoUnmarshaller<NotificationSet> {

    /**
     * A registry of named workflow elements.
     */
    private HeterogeneousRegistry registry;

    /**
     * @param registry the registry of named workflow elements.
     */
    public TitoNotificationSetUnmarshaller(HeterogeneousRegistry registry) {
        this.registry = registry == null ? new NullHeterogeneousRegistry() : registry;
    }

    /**
     * Creates a new notification set using values from the given JSON object.
     *
     * @param json the JSON object.
     * @return the notification set.
     * @throws JSONException if the JSON object is missing a required field or contains an unexpected type.
     */
    public NotificationSet fromJson(JSONObject json) throws JSONException {
        NotificationSet notificationSet = new NotificationSet();
        notificationSet.setIdc(ImportUtils.getId(json, "id"));
        notificationSet.setName(json.getString("name"));
        notificationSet.setTemplate_id(getAnalysisId(json));
        notificationSet.setNotifications(notificationListFromJson(json.getJSONArray("wizard_notifications")));
        return notificationSet;
    }

    /**
     * Gets the analysis ID for the notification set that is being imported. If the analysis ID is known in advance, it
     * can be specified directly using the "analysis_id" element. If the analysis ID is not known in advance but is
     * in an analysis registry that was provided to this importer then the analysis name can be specified using the
     * "analysis_ref" element.
     *
     * @param json the JSON object representing the notification set.
     * @return the analysis ID.
     * @throws JSONException if the analysis ID can't be obtained for any reason.
     */
    private String getAnalysisId(JSONObject json) throws JSONException {
        String analysisId = json.optString("analysis_id", null);
        if (analysisId == null) {
            analysisId = getNamedAnalysisId(json.optString("analysis_ref", null));
        }
        if (analysisId == null) {
            String msg = "unable to determine the analysis ID for the notification set; please verify that the "
                + "\"analysis_id\" or \"analysis_ref\" element is specified correctly";
            throw new JSONException(msg);
        }
        return analysisId;
    }

    /**
     * Gets the analysis ID of a named analysis in the analysis registry.
     *
     * @param analysisName the name of the analysis.
     * @return the analysis ID or null if the analysis can't be found.
     */
    private String getNamedAnalysisId(String analysisName) {
        TransformationActivity analysis = null;
        if (analysisName != null) {
            analysis = registry.get(TransformationActivity.class, analysisName);
        }
        return analysis == null ? null : analysis.getId();
    }

    /**
     * Creates a new notification list from the given JSON array.
     *
     * @param array the JSON array.
     * @return the new notification list.
     * @throws JSONException if any object in the JSON array is missing a required field or contains an unexpected type.
     */
    private List<Notification> notificationListFromJson(JSONArray array) throws JSONException {
        List<Notification> notifications = new LinkedList<Notification>();
        for (int i = 0; i < array.length(); i++) {
            notifications.add(notificationFromJson(array.getJSONObject(i)));
        }
        return notifications;
    }

    /**
     * Creates a new notification from the given JSON object.
     *
     * @param json the JSON object.
     * @return the new notification.
     * @throws JSONException if the JSON object is missing a required field or contains an unexpected type.
     */
    private Notification notificationFromJson(JSONObject json) throws JSONException {
        Notification notification = new Notification();
        notification.setIdc(ImportUtils.getId(json, "id"));
        notification.setName(json.optString("name", null));
        notification.setSender(json.getString("sender"));
        notification.setType(json.getString("type"));
        notification.setReceivers(receiversFromJson(json.getJSONArray("receivers")));
        return notification;
    }

    /**
     * Creates a new receiver list from the given JSON array.
     *
     * @param array the JSON array.
     * @return the new receiver list.
     * @throws JSONException if any element in the array contains a value with an unexpected type.
     */
    private List<String> receiversFromJson(JSONArray array) throws JSONException {
        List<String> receivers = new LinkedList<String>();
        for (int i = 0; i < array.length(); i++) {
            receivers.add(array.getString(i));
        }
        return receivers;
    }
}
