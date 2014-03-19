package org.iplantc.workflow.template.notifications;

import java.util.LinkedList;
import java.util.List;

import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.workflow.marshaler.NotificationMarshaller;

public class NotificationSet implements NamedAndUnique {

    long hid;

    String idc;

    String name;

    String template_id;

    List<Notification> notifications = new LinkedList<Notification>();

    @Override
    public String getId() {
        return idc;
    }

    public String getIdc() {
        return idc;
    }

    public void setIdc(String id) {
        this.idc = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_name) {
        this.template_id = template_name;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void addNotification(Notification notification) {
        notifications.add(notification);
    }

    public void accept(NotificationMarshaller marshaller) throws Exception {
        marshaller.visit(this);

        for (Notification notification : notifications) {
            notification.accept(marshaller);
        }

        marshaller.leave(this);
    }

    public long getHid() {
        return hid;
    }

    public void setHid(long hid) {
        this.hid = hid;
    }
}
