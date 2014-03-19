package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.persistence.RepresentableAsJson;
import org.iplantc.workflow.WorkflowException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the type of value (for example, String, Number or Boolean) that may be stored in properties of a given
 * type or validated by validation rules of a given type.
 * 
 * @author Dennis Roberts
 */
public class ValueType implements RepresentableAsJson, NamedAndUnique {

    /**
     * Used to identify the value type in the database.
     */
    private long hid;

    /**
     * A UUID used to identify the value type outside of the database.
     */
    private String id;

    /**
     * The name of the value type.
     */
    private String name;

    /**
     * A brief description of the value type.
     */
    private String description;

    /**
     * @return the value used to identify the value type in the database.
     */
    public long getHid() {
        return hid;
    }

    /**
     * @param hid the value used to identify the value type in the database.
     */
    public void setHid(long hid) {
        this.hid = hid;
    }

    /**
     * @return the value used to identify the value type outside of the database.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @param id the value used to identify the value type outside of the database.
     */
    public void setId(String id) {
        validateFieldLength(this.getClass(), "id", id, 40);
        this.id = id;
    }

    /**
     * @return the name of the value type.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name of the value type.
     */
    public void setName(String name) {
        validateFieldLength(this.getClass(), "name", name, 40);
        this.name = name;
    }

    /**
     * @return a brief description of the value type.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description a brief description of the value type.
     */
    public void setDescription(String description) {
        validateFieldLength(this.getClass(), "description", description, 255);
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("hid", hid);
            json.put("id", id);
            json.put("name", name);
            json.put("description", description);
            return json;
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to format the JSON object", e);
        }
    }
}
