package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.persistence.RepresentableAsJson;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single generic element in a workflow. This abstract class contains all of the properties that are common
 * to all elements in a workflow.
 *
 * @author Dennis Roberts
 */
public abstract class WorkflowElement implements RepresentableAsJson, NamedAndUnique {

    /**
     * The workflow identifier. This needs to be a long because Hibernate dictates that it must be. Note that the
     * identifier is not unique among all workflow elements, but only among all workflow elements of the same kind For
     * example, a template and a property type may share the same identifier, but two templates cannot share the same
     * identifier.
     */
    private long hid;

    private String id;

    /**
     * The name of the workflow element. This name should be unique, but it is not used as the primary key.
     */
    private String name = "";

    /**
     * The label to use when displaying the workflow element to the user.
     */
    private String label = "";

    /**
     * A brief description of the workflow element. This should be slightly more descriptive than the label so that it
     * can be used as tooltip text.
     */
    private String description;

    /**
     * Sets the workflow element identifier.
     *
     * @param id the new identifier.
     */
    public void setId(String id) {
        validateFieldLength(this.getClass(), "id", id, 255);
        this.id = id;
    }

    /**
     * Gets the workflow element identifier.
     *
     * @return the identifier.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the workflow element name, which is a unique string used to identify the element.
     *
     * @param name the new name.
     */
    public void setName(String name) {
        validateFieldLength(this.getClass(), "name", name, 255);
        this.name = name;
    }

    /**
     * Gets the workflow element name, which is a unique string used to identify the element.
     *
     * @return the name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the workflow element label, which is used when the element is displayed to the user.
     *
     * @param label the label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the workflow element label, which is used when the element is displayed to the user.
     *
     * @return the label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the workflow element description, which may be used as tooltip text for the element.
     *
     * @param description the description.
     */
    public void setDescription(String description) {
        validateFieldLength(this.getClass(), "description", description, 255);
        this.description = description;
    }

    /**
     * Gets the workflow element description, which may be used as tooltip text for the element.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Creates a new empty workflow element.
     */
    protected WorkflowElement() {
    }

    /**
     * Creates a new workflow element with the given member values.
     *
     * @param id the workflow element identifier.
     * @param name the workflow element name.
     * @param label the workflow element label.
     * @param description the workflow element description.
     */
    protected WorkflowElement(String id, String name, String label, String description) {
        validateFieldLength(this.getClass(), "id", id, 255);
        validateFieldLength(this.getClass(), "name", name, 255);
        validateFieldLength(this.getClass(), "description", description, 255);
        this.id = id;
        this.name = name;
        this.label = label;
        this.description = description;
    }

    /**
     * Accepts a visit from a workflow marshaller.
     *
     * @param marshaller the marshaller that is visiting.
     * @throws WorkflowException if an error occurs.
     */
    public abstract void accept(BaseTemplateMarshaller marshaller) throws WorkflowException;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof WorkflowElement) {
            WorkflowElement other = (WorkflowElement) otherObject;
            if (!StringUtils.equals(other.getId(), id)) {
                return false;
            }
            if (!StringUtils.equals(other.getName(), name)) {
                return false;
            }
            if (!StringUtils.equals(other.getLabel(), label)) {
                return false;
            }
            if (!StringUtils.equals(other.getDescription(), description)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = getClass().getName().hashCode();
        hashCode += ObjectUtils.hashCode(id);
        hashCode += ObjectUtils.hashCode(name);
        hashCode += ObjectUtils.hashCode(label);
        hashCode += ObjectUtils.hashCode(description);
        return hashCode;
    }

    public long getHid() {
        return hid;
    }

    public void setHid(long hid) {
        this.hid = hid;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("label", label);
            json.put("description", description);
            json.put("hid", hid);
            return json;
        } catch (JSONException e) {
            throw new WorkflowException("unable to format the JSON object", e);
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
