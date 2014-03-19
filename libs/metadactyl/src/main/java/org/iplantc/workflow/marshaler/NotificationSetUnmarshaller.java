package org.iplantc.workflow.marshaler;




import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationSetUnmarshaller {



	public NotificationSet unmarshallNotificationSet(JSONObject json) throws JSONException{

		NotificationSet set =new NotificationSet();

		set.setIdc(json.getString("id"));
		set.setName(json.getString("name"));
		set.setTemplate_id(json.getString("template_id"));

		JSONArray array = json.getJSONArray("wizardNotifications");


		for(int i=0; i < array.length(); i++){
			JSONObject element = array.getJSONObject(i);
			Notification notification = unmarshallNotification(element);
			set.addNotification(notification);
		}



		return set;

	}


	public Notification unmarshallNotification(JSONObject json) throws JSONException{

		Notification notification = new Notification();

		//notification.setId(json.getString("id"));
		//notification.setName(json.getString("name"));
		notification.setIdc("temporary_id");
		notification.setName("temporary_name");
		notification.setSender(json.getString("sender"));
		notification.setType(json.getString("type"));

		JSONArray receivers = json.getJSONArray("receivers");

		for(int i=0; i < receivers.length();i++){
			notification.addreceiver(receivers.getString(i));
		}

		return notification;
	}



}
