package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single property, which is a value that may change and is typically specified by the user, in a
 * workflow. The most common use of a property is to allow the user to specify a value of an argument to be passed to
 * one of the jobs in the workflow.
 *
 * @author Dennis Roberts
 */
public class Property extends WorkflowElement {

    /**
     * The type of the property.
     */
    private PropertyType propertyType;

    /**
     * Used to validate the property.
     */
    private Validator validator;

    private int order;

    /**
     * True if the property should be visible in the user interface.
     */
    private boolean isVisible;

    /**
     * The value of the property.
     */
    private String defaultValue;

    /**
     * If true, the property will be omitted from the job submission if its value is blank.
     */
    private boolean omitIfBlank;

    /**
     * The DataObject associated with a property of type "Input" or "Output".
     */
    private DataObject dataObject;

    /**
     * Sets the type of the property.
     *
     * @param propertyType the new property type.
     */
    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    /**
     * Gets the type of the property.
     *
     * @return the property type.
     */
    public PropertyType getPropertyType() {
        return propertyType;
    }

    /**
     * @return the name of the property type.
     */
    public String getPropertyTypeName() {
        return propertyType == null ? "" : propertyType.getName();
    }

    /**
     * @return the property type name to use for output properties.
     */
    public String getOutputTypeName() {
        return dataObject == null ? "" : dataObject.getOutputTypeName();
    }

    /**
     * Sets the validator to use when validating objects.
     *
     * @param validator the new validator.
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Gets the validator to use when validating property values.
     *
     * @return the validator.
     */
    public Validator getValidator() {
        return validator;
    }

    /**
     * Sets the visibility flag for this property.
     *
     * @param isVisible the new visibility flag.
     */
    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /**
     * Gets the visibility flag for this property.
     *
     * @return the visibility flag.
     */
    public boolean getIsVisible() {
        return isVisible;
    }

    /**
     * Sets the property value.
     *
     * @param value the new property value.
     */
    public void setDefaultValue(String value) {
        validateFieldLength(this.getClass(), "defaultValue", value, 255);
        this.defaultValue = value;
    }

    /**
     * Gets the property value.
     *
     * @return the property value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the flag indicating if the property should be omitted from job submissions if its values is blank.
     */
    public boolean getOmitIfBlank() {
        return omitIfBlank;
    }

    /**
     * @param omitIfBlank if true, the property will be omitted from job submissions if its values is blank.
     */
    public void setOmitIfBlank(boolean omitIfBlank) {
        this.omitIfBlank = omitIfBlank;
    }

    /**
     * Sets the input/output DataObject.
     *
     * @param dataObject the new DataObject.
     */
    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }

    /**
     * Gets the input/output DataObject.
     *
     * @return the input/output DataObject.
     */
    public DataObject getDataObject() {
        return dataObject;
    }

    /**
     * Creates a new empty property.
     */
    public Property() {
        super();
    }

    /**
     * Creates a new property with the given ID, name, label and description.
     *
     * @param id the property identifier.
     * @param name the property name.
     * @param label the property label.
     * @param description the property description.
     */
    public Property(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);

        if (validator != null) {
            validator.accept(marshaller);
        }
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof Property) {
            Property other = (Property) otherObject;
            if (!super.equals(other)) {
                return false;
            }
            if (!ObjectUtils.equals(propertyType, other.getPropertyType())) {
                return false;
            }
            if (!ObjectUtils.equals(validator, other.getValidator())) {
                return false;
            }
            if (isVisible != other.getIsVisible()) {
                return false;
            }
            if (!StringUtils.equals(defaultValue, other.getDefaultValue())) {
                return false;
            }
            if (omitIfBlank != other.getOmitIfBlank()) {
                return false;
            }
            if (!ObjectUtils.equals(dataObject, other.getDataObject())) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode += ObjectUtils.hashCode(propertyType);
        hashCode += ObjectUtils.hashCode(validator);
        hashCode += new Boolean(isVisible).hashCode();
        hashCode += ObjectUtils.hashCode(defaultValue);
        hashCode += new Boolean(omitIfBlank).hashCode();
        hashCode += ObjectUtils.hashCode(dataObject);
        return hashCode;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = super.toJson();
            json.put("propertyType", propertyType.toJson());
            json.put("data_object", dataObject.toJson());
            return json;
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to format the JSON object", e);
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * Creates a copy of an existing property.
     *
     * @param orig the original property.
     * @return the copy.
     */
    public static Property copy(Property orig) {
        Property copy = new Property(orig.getId(), orig.getName(), orig.getLabel(), orig.getDescription());
        copy.propertyType = orig.propertyType;
        copy.validator = orig.validator;
        copy.order = orig.order;
        copy.isVisible = orig.isVisible;
        copy.defaultValue = orig.defaultValue;
        copy.omitIfBlank = orig.omitIfBlank;
        return copy;
    }
}
