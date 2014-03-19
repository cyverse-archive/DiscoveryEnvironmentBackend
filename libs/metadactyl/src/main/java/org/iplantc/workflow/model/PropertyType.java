package org.iplantc.workflow.model;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the type of a property in a workflow. A workflow property is a value that can change, and the user is
 * typically prompted for the value of each property. The property type determines the general type of information
 * that the property may hold and how the user is prompted for property values. Some examples of basic property types
 * are <code>text</code> and <code>boolean</code>. A <code>text</code> property would normally be represented by a
 * textbox in a form and could contain any arbitrary text. A <code>boolean<code> property, on the other hand, would
 * normally be represented by a checkbox in a form and may contain only true and false values.
 *
 * @author Dennis Roberts
 */
public class PropertyType extends WorkflowElement {

    /**
     * True if this property type has been deprecated.
     */
    private boolean deprecated;

    /**
     * True if properties of this type may be hidden.
     */
    private boolean hidable;

    /**
     * Used to specify the display order relative to other property types.
     */
    private int displayOrder;

    /**
     * The type of value that this property type represents.
     */
    private ValueType valueType;

    /**
     * @param deprecated the new deprecated flag.
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * @return true if the property type is deprecated.
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @param hidable the new hidable flag.
     */
    public void setHidable(boolean hidable) {
        this.hidable = hidable;
    }

    /**
     * @return true if properties of this type can be hidden.
     */
    public boolean isHidable() {
        return hidable;
    }

    /**
     * @param displayOrder the new display order.
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * @return the display order.
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * @param valueType the new value type.
     */
    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    /**
     * @return the value type.
     */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * Creates a new empty property type.
     */
    public PropertyType() {
        super();
    }

    /**
     * Creates a new property type with the given ID, name, label, and description.
     *
     * @param id the property type identifier.
     * @param name the property type name.
     * @param label the property type label.
     * @param description the property type description.
     */
    public PropertyType(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = super.toJson();
            json.put("value_type", valueType == null ? "Unspecified" : valueType.getName());
            return json;
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to format the JSON object", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof PropertyType) {
            return super.equals(otherObject);
        }
        return false;
    }
}
