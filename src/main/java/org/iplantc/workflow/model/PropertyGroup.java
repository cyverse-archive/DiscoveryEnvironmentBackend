package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;

/**
 * Represents a group of properties in a workflow. Each property group typically gets its own pane in the user
 * interface.
 *
 * @author Dennis Roberts
 */
public class PropertyGroup extends WorkflowElement implements Iterable<Property> {

    /**
     * The list of properties in this property group.
     */
    private List<Property> properties = new LinkedList<Property>();

    /**
     * The type of property group
     */
    private String groupType;

    private boolean visible=true;

    /**
     * Creates a new property group.
     */
    public PropertyGroup() {
        super();
    }

    /**
     * Creates a new property group with the given ID, name, label and description.
     *
     * @param id the property group identifier.
     * @param name the property group name.
     * @param label the property group label.
     * @param description the property group description.
     */
    public PropertyGroup(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * Adds a property to this property group.
     *
     * @param property the property to add.
     */
    public void addProperty(Property property) {
        properties.add(property);
    }

    /**
     * Gets an iterator for the properties in this property group.
     *
     * @return the iterator.
     */
    @Override
    public Iterator<Property> iterator() {
        return properties.iterator();
    }

    /**
     * Sets the list of properties. Hibernate requires a setter.
     *
     * @param properties the new list of properties.
     */
    public void setProperties(List<Property> properties) {
        this.properties = new LinkedList<Property>(properties);
    }

    /**
     * Gets the type of property group
     *
     * @return the type of this property group.
     */
    public String getGroupType() {
        return groupType;
    }

    /**
     * Sets the type of this property groups
     *
     * @param pgtype the name of the property group type.
     */
    public void setGroupType(String pgtype) {
        validateFieldLength(this.getClass(), "type", pgtype, 255);
        groupType = pgtype;
    }

    /**
     * Gets the list of properties.
     *
     * @return an unmodifiable copy of the list of properties.
     */
    public List<Property> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);
        for (Property property : properties) {
            property.accept(marshaller);
        }
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof PropertyGroup) {
            PropertyGroup other = (PropertyGroup) otherObject;
            if (!super.equals(other)) {
                return false;
            }
            if (!ObjectUtils.equals(properties, other.getProperties())) {
                return false;
            }
            if (!StringUtils.equals(groupType, other.getGroupType())) {
                return false;
            }
            return true;
        }
        return false;
    }

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode += ObjectUtils.hashCode(properties);
        hashCode += ObjectUtils.hashCode(groupType);
        return hashCode;
    }
}
