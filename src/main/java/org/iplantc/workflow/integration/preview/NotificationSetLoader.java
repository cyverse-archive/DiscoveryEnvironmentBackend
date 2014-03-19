package org.iplantc.workflow.integration.preview;

import org.iplantc.workflow.integration.json.TitoNotificationSetUnmarshaller;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to load notification sets that are represented by JSON objects into memory.
 * 
 * @author Dennis Roberts
 */
public class NotificationSetLoader extends ObjectLoader {

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadObject(JSONObject json) throws JSONException {
        TitoNotificationSetUnmarshaller unmarshaller = new TitoNotificationSetUnmarshaller(getNameRegistry());
        if (json != null) {
            loadNotificationSet(unmarshaller, json);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadObjectList(JSONArray array) throws JSONException {
        TitoNotificationSetUnmarshaller unmarshaller = new TitoNotificationSetUnmarshaller(getNameRegistry());
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                loadNotificationSet(unmarshaller, json);
            }
        }
    }

    /**
     * Loads a notification set into memory using the given unmarshaller.
     * 
     * @param unmarshaller used to convert the JSON object to a notification set.
     * @param json the JSON object.
     * @throws JSONException if the JSON object doesn't meet the requirements of the importer.
     */
    private void loadNotificationSet(TitoNotificationSetUnmarshaller unmarshaller, JSONObject json)
        throws JSONException
    {
        NotificationSet notificationSet = unmarshaller.fromJson(json);
        registerByName(NotificationSet.class, notificationSet.getName(), notificationSet);
        registerById(NotificationSet.class, notificationSet.getId(), notificationSet);
    }
}
