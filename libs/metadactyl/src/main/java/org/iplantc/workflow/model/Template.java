package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Transient;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;

/**
 * A template used to describe the information necessary to submit a workflow to the discovery environment. A
 * workflow consists of groups of properties, which are typically arguments that need to be provided to one or
 * more jobs in the workflow and whose values may be (but are not necessarily) supplied by the user. Templates
 * may also be nested within each other in order to create complex hierarchical relationships and to provide
 * useful features such as property inheritance.
 *
 * @author Dennis Roberts
 */
public class Template extends WorkflowElement {

    /**
     * The list of property groups.
     */
    private List<PropertyGroup> propertyGroups = new LinkedList<PropertyGroup>();
    private List<DataObject> inputs = new LinkedList<DataObject>();
    private List<DataObject> outputs = new LinkedList<DataObject>();

    /** the id for the associated deployed component **/
    private String component;

    /**
     * The type of this template.
     */
    private String templateType;

    @Transient
    private Date integrationDate;

    @Transient
    private Date editedDate;

    /**
     * Adds a property group to the list of property groups.
     *
     * @param propertyGroup the property group to add.
     */
    public void addPropertyGroup(PropertyGroup propertyGroup) {
        propertyGroups.add(propertyGroup);
    }

    /**
     * Adds a property group to the list of property groups.
     *
     * @param propertyGroup the property group to add.
     */
    public void addPropertyGroup(int index, PropertyGroup propertyGroup) {
        propertyGroups.add(index, propertyGroup);
    }

    /**
     * Sets the list of property groups. Hibernate requires a setter.
     *
     * @param propertyGroups the new list of property groups.
     */
    public void setPropertyGroups(List<PropertyGroup> propertyGroups) {
        this.propertyGroups = new LinkedList<PropertyGroup>(propertyGroups);
    }

    /**
     * Gets the list of property groups.
     *
     * @return the list of property groups.
     */
    public List<PropertyGroup> getPropertyGroups() {
        return propertyGroups;
    }

    /**
     * Sets the type of this template
     *
     *
     * @param template_type the new type of this template
     */
    public void setTemplateType(String template_type) {
        validateFieldLength(this.getClass(), "type", template_type, 255);
        templateType = template_type;
    }

    /**
     * Gets the type for this template
     */
    public String getTemplateType() {
        return templateType;
    }

    /**
     * Sets the type of this template
     *
     * @param ttype the type of the template
     */
    public void setType(String ttype) {
        validateFieldLength(this.getClass(), "type", ttype, 255);
        templateType = ttype;
    }

    public Date getIntegrationDate() {
        return integrationDate;
    }

    public void setIntegrationDate(Date integrationDate) {
        this.integrationDate = integrationDate;
    }

    public Date getEditedDate() {
        return editedDate;
    }

    public void setEditedDate(Date editedDate) {
        this.editedDate = editedDate;
    }

    /**
     * Creates a new empty template.
     */
    public Template() {
        super();
    }

    /**
     * Creates a new template with the given ID, name, label and description.
     *
     * @param id the template identifier.
     * @param name the template name.
     * @param label the template label.
     * @param description the template description.
     */
    public Template(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        try {
            marshaller.visit(this);

            marshaller.visitInputs(inputs);
            marshaller.leaveInputs(inputs);

            for (PropertyGroup propertyGroup : propertyGroups) {
                propertyGroup.accept(marshaller);
            }
            marshaller.leave(this);
        }
        catch (Exception ex) {
            throw new WorkflowException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof Template) {
            Template other = (Template) otherObject;
            if (!super.equals(other)) {
                return false;
            }
            if (!ObjectUtils.equals(propertyGroups, other.getPropertyGroups())) {
                return false;
            }
            if (!StringUtils.equals(templateType, other.getTemplateType())) {
                return false;
            }
            if (!ObjectUtils.equals(inputs, other.getInputs())) {
                return false;
            }
            if (!ObjectUtils.equals(outputs, other.getOutputs())) {
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
        int hashCode = super.hashCode();
        hashCode += ObjectUtils.hashCode(propertyGroups);
        hashCode += ObjectUtils.hashCode(templateType);
        hashCode += ObjectUtils.hashCode(inputs);
        hashCode += ObjectUtils.hashCode(outputs);
        return hashCode;
    }

    public List<DataObject> getInputs() {
        return inputs;
    }

    public void setInputs(List<DataObject> inputs) {
        this.inputs = inputs;
    }

    public List<DataObject> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<DataObject> outputs) {
        this.outputs = outputs;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        validateFieldLength(this.getClass(), "component_id", component, 255);
        this.component = component;
    }

    /**
     * Returns true if the property with the specified name is an input
     * DataObject
     *
     * @param name the property to test for
     * @return true if the property is an input dataobject, false otherwise.
     */

    public boolean isInput(String name) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).getName().equals(name))
                return true;
        }
        return false;
    }

    public void addInputObject(DataObject input) {
        inputs.add(input);
    }

    public void addOutputObject(DataObject output) {
        outputs.add(output);
    }

    public String getOutputName(String name) throws Exception {

        for (DataObject output : outputs) {
            if (output.getId().equals(name))
                return output.getName();
        }

        throw new Exception("This template does not contain an output property called " + name);
    }

    public boolean hasOutputObject(String id) {

        for (DataObject output : outputs) {
            if (output.getId().equals(id))
                return true;
        }
        return false;
    }

    public boolean hasInputObject(String id) {
        for (DataObject input : inputs) {
            if (input.getId().equals(id))
                return true;
        }
        return false;
    }

    /**
     * Finds the list of data objects that are associated with properties in this template.
     *
     * @return the list of referenced data objects.
     */
    public List<DataObject> findReferencedDataObjects() {
        List<DataObject> result = new ArrayList<DataObject>();
        for (PropertyGroup propertyGroup : getPropertyGroups()) {
            for (Property property : propertyGroup.getProperties()) {
                DataObject dataObject = property.getDataObject();
                if (dataObject != null) {
                    result.add(dataObject);
                }
            }
        }
        return result;
    }

    /**
     * Finds the list of inputs that are not referenced by any property in the template.
     *
     * @return the list of unreferenced inputs.
     */
    public List<DataObject> findUnreferencedInputs() {
        return ListUtils.subtract(inputs, findReferencedDataObjects());
    }

    /**
     * Finds the list of outputs that are not referenced by any property in the template.
     *
     * @return the list of unreferenced outputs.
     */
    public List<DataObject> findUnreferencedOutputs() {
        return ListUtils.subtract(outputs, findReferencedDataObjects());
    }
}
