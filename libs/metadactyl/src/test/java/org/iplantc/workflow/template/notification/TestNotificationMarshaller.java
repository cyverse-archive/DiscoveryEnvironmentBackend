package org.iplantc.workflow.template.notification;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.iplantc.workflow.marshaler.NotificationMarshaller;
import org.iplantc.workflow.template.notifications.Notification;
import org.iplantc.workflow.template.notifications.NotificationSet;


import junit.framework.TestCase;

public class TestNotificationMarshaller extends TestCase{


	public void testMarshallNotificationSet() throws Exception{

		NotificationSet set1 = new NotificationSet();

		set1.setIdc("none");
		set1.setName("");
		set1.setTemplate_id("t12af368916d33352e31302e3231d01170012afe3c9");

		Notification notification1 = new Notification();

		notification1.setSender("me");
		notification1.setType("disableOnSelection");

		notification1.addreceiver("barcodeEntryOption");
		notification1.addreceiver("numberOfAllowedMismatches");

		Notification notification2 = new Notification();

		notification2.setSender("me");
		notification2.setType("disableOnSelection");

		notification2.addreceiver("barcodeEntryOption");
		notification2.addreceiver("numberOfAllowedMismatches");

		set1.addNotification(notification1);

		NotificationMarshaller marshaller = new NotificationMarshaller();

		set1.accept(marshaller);

		String json_set = marshaller.getMarshalledWorkflow();


		JSONObject jset = (JSONObject) JSONSerializer.toJSON(json_set);

		JSONArray array = jset.getJSONArray("wizardNotifications");

		assertEquals(1, array.size());



	}

}
