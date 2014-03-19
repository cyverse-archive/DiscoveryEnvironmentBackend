package org.iplantc.workflow.template.notifications;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.LinkedList;
import java.util.List;

import org.iplantc.workflow.marshaler.NotificationMarshaller;

public class Notification {

	long hid;

	String idc;
	String name;
	String sender;
	String type;
	List<String> receivers = new LinkedList<String>();


	public String getIdc() {
		return idc;
	}
	public void setIdc(String id) {
        validateFieldLength(this.getClass(), "id", id, 255);
		this.idc = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
        validateFieldLength(this.getClass(), "name", name, 255);
		this.name = name;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
        validateFieldLength(this.getClass(), "sender", sender, 255);
		this.sender = sender;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
        validateFieldLength(this.getClass(), "type", type, 255);
		this.type = type;
	}
	public List<String> getReceivers() {
		return receivers;
	}
	public void setReceivers(List<String> receivers) {
        for (int i = 0; i < receivers.size(); i++) {
            validateFieldLength(this.getClass(), "receivers[" + i + "]", receivers.get(i), 255);
        }
		this.receivers = receivers;
	}

	public void addreceiver(String receiver){
		receivers.add(receiver);
	}


	public void accept(NotificationMarshaller marshaller) throws Exception{
		marshaller.visit(this);
		marshaller.leave(this);

	}


	public long getHid() {
		return hid;
	}
	public void setHid(long hid) {
		this.hid = hid;
	}




}
