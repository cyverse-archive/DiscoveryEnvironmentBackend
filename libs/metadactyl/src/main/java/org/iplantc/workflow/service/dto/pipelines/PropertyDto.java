package org.iplantc.workflow.service.dto.pipelines;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object used to describe a property in a pipeline.
 * 
 * @author Dennis Roberts
 */
public class PropertyDto extends AbstractDto {

    /**
     * The property identifier.
     */
    @JsonField(name = "id")
    private String id;

    /**
     * The property name.
     */
    @JsonField(name = "name")
    private String name;

    /**
     * The property label.
     */
    @JsonField(name = "label")
    private String label;

    /**
     * The property description.
     */
    @JsonField(name = "description")
    private String description;

    /**
     * True if the property is visible in the UI.
     */
    @JsonField(name = "isVisible")
    private boolean visible;

    /**
     * The default value of the property.
     */
    @JsonField(name = "value", optional = true)
    private String defaultValue;

    /**
     * The property type.
     */
    @JsonField(name = "type")
    private String typeName;

    /**
     * The data object associated with this property.
     */
    @JsonField(name = "data_object", optional = true)
    private DataObjectDto dataObject;

    /**
     * @return the property identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the property name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the property label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if the property is visible in the UI.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return the default value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the property type name.
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return the data object associated with this property.
     */
    public DataObjectDto getDataObject() {
        return dataObject;
    }

    /**
     * @param property the source property.
     */
    public PropertyDto(Property property) {
        id = StringUtils.defaultString(property.getId());
        name = StringUtils.defaultString(property.getName());
        label = StringUtils.defaultString(property.getLabel());
        description = StringUtils.defaultString(property.getDescription());
        visible = property.getIsVisible();
        defaultValue = property.getDefaultValue();
        typeName = property.getPropertyTypeName();
        dataObject = DataObjectDto.fromDataObject(property.getDataObject());
    }

    /**
     * @param json a JSON object representing the property.
     */
    public PropertyDto(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str a JSON string representing the property.
     */
    public PropertyDto(String str) {
        fromString(str);
    }

    /**
     * @param source the data object to generate the property from.
     */
    private PropertyDto(DataObject source, String type) {
        id = StringUtils.defaultString(source.getId());
        name = StringUtils.defaultString(source.getSwitchString());
        label = firstNonBlank("", source.getLabel(), source.getName(), source.getSwitchString());
        description = StringUtils.defaultString(source.getDescription());
        visible = true;
        defaultValue = null;
        typeName = type;
        dataObject = new DataObjectDto(source);
    }

    /**
     * Returns the first string that is not blank in one or more strings.
     * 
     * @param defaultStr the value to return if all of the strings are blank.
     * @param strs the strings.
     * @return the first string that isn't blank or the default if all of the strings are blank.
     */
    private String firstNonBlank(String defaultStr, String... strs) {
        for (String str : strs) {
            if (!StringUtils.isBlank(str)) {
                return str;
            }
        }
        return defaultStr;
    }

    /**
     * Creates a property DTO for an input data object.
     * 
     * @param input the input data object.
     * @return the property DTO.
     */
    public static PropertyDto fromInput(DataObject input) {
        return new PropertyDto(input, "Input");
    }

    /**
     * Creates a property DTO for an output data object.
     * 
     * @param output the output data object.
     * @return the property DTO.
     */
    public static PropertyDto fromOutput(DataObject output) {
        return new PropertyDto(output, "Output");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PropertyDto other = (PropertyDto) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.label == null) ? (other.label != null) : !this.label.equals(other.label)) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        if (this.visible != other.visible) {
            return false;
        }
        if ((this.defaultValue == null) ? (other.defaultValue != null) : !this.defaultValue.equals(other.defaultValue)) {
            return false;
        }
        if ((this.typeName == null) ? (other.typeName != null) : !this.typeName.equals(other.typeName)) {
            return false;
        }
        if (this.dataObject != other.dataObject
                && (this.dataObject == null || !this.dataObject.equals(other.dataObject))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 83 * hash + (this.label != null ? this.label.hashCode() : 0);
        hash = 83 * hash + (this.description != null ? this.description.hashCode() : 0);
        hash = 83 * hash + (this.visible ? 1 : 0);
        hash = 83 * hash + (this.defaultValue != null ? this.defaultValue.hashCode() : 0);
        hash = 83 * hash + (this.typeName != null ? this.typeName.hashCode() : 0);
        hash = 83 * hash + (this.dataObject != null ? this.dataObject.hashCode() : 0);
        return hash;
    }
}
