package org.iplantc.workflow.marshaler;

import java.util.List;

import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationMarshaller extends BaseMarshaller{
	public void visit(NotificationSet set) {
        JSONObject json = createJsonObject();
        jsonStack.push(json);
	}

	public void leave(NotificationSet set){
		jsonStack.pop();
	}

	public void visit(Notification notification) throws JSONException {
        JSONObject json =createJsonObject();

        json.put("sender", notification.getSender());
        json.put("type", notification.getType());

        List<String> receivers = notification.getReceivers();

        JSONArray array = new JSONArray();

        for(int i=0; i < receivers.size(); i++){
            array.put(receivers.get(i));
        }

        json.put("receivers",array);

        appendToParentProperty("wizardNotifications", json);

        jsonStack.push(json);
	}

	public void leave(Notification notification){
		jsonStack.pop();
	}
}
