package org.iplantc.workflow.integration.json;

import static org.iplantc.workflow.integration.util.JsonUtils.collectionToJsonArray;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to convert an existing notification set to a JSON document.
 * 
 * @author Dennis Roberts
 */
public class TitoNotificationSetMarshaller implements TitoMarshaller<NotificationSet> {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * True if the JSON produced by this marshaller should use backward references.
     */
    private boolean useReferences;

    /**
     * Initializes a notification set marshaller that does not produce JSON that uses backward references. A DAO
     * factory is not required if backward references are not used.
     */
    public TitoNotificationSetMarshaller() {
        this(null, false);
    }

    /**
     * @param daoFactory used to obtain data access objects.
     * @param useReferences true if the marshaller should produce JSON that uses backward references.
     */
    public TitoNotificationSetMarshaller(DaoFactory daoFactory, boolean useReferences) {
        this.daoFactory = daoFactory;
        this.useReferences = useReferences;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson(NotificationSet object) {
        try {
            return marshalNotificationSet(object);
        }
        catch (JSONException e) {
            throw new WorkflowException("error producing JSON object", e);
        }
    }

    /**
     * Marshals a single notification set.
     * 
     * @param notificationSet the notification set to marshal.
     * @return the marshaled notification set.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalNotificationSet(NotificationSet notificationSet) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", notificationSet.getIdc());
        json.put("name", notificationSet.getName());
        if (useReferences) {
            json.put("analysis_ref", getAnalysisName(notificationSet.getTemplate_id()));
        }
        else {
            json.put("analysis_id", notificationSet.getTemplate_id());
        }
        json.put("wizard_notifications", marshalNotifications(notificationSet.getNotifications()));
        return json;
    }

    /**
     * Gets the name of the analysis with the given identifier.
     * 
     * @param analysisId the analysis identifier.
     * @return the analysis name.
     */
    private String getAnalysisName(String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("no analysis with ID, " + analysisId + ", found");
        }
        return analysis.getName();
    }

    /**
     * Marshals the list of notifications.
     * 
     * @param notifications the list of notifications.
     * @return the marshaled notification list.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONArray marshalNotifications(List<Notification> notifications) throws JSONException {
        JSONArray array = new JSONArray();
        for (Notification notification : notifications) {
            array.put(marshalNotification(notification));
        }
        return array;
    }

    /**
     * Marshals a single notification.
     * 
     * @param notification the notification.
     * @return the marshaled notification.
     * @throws JSONException if a JSON error occurs.
     */
    private JSONObject marshalNotification(Notification notification) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("sender", notification.getSender());
        json.put("type", notification.getType());
        json.put("receivers", collectionToJsonArray(notification.getReceivers()));
        return json;
    }
}
