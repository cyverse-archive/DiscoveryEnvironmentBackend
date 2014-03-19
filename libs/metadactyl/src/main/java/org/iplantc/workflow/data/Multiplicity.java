package org.iplantc.workflow.data;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.iplantc.workflow.model.WorkflowElement;

/**
 * Indicates the number and packaging of input or output files.
 *
 * @author Dennis Roberts
 */
public class Multiplicity extends WorkflowElement {

    /**
     * The type of widget used to allow users to select values for input fields with this multiplicity.
     */
    private String typeName;

    /**
     * The type of widget used to allow users to specify file names for output fields with this multiplicity.
     */
    private String outputTypeName;

    /**
     * Sets the type of widget used to allow users to select values for input fields with this multiplicity.
     *
     * @param typeName the new widget type.
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Gets the type of widget used to allow users to select values for input fields with this multiplicity.
     *
     * @return the widget type.
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets the type of widget used to allow users to specify file names for output fields with this multiplicity.
     *
     * @param outputTypeName the new widget type.
     */
    public void setOutputTypeName(String outputTypeName) {
        this.outputTypeName = outputTypeName;
    }

    /**
     * Gets the type of widget used to allow users to specify file names for output fields with this multiplicity.
     *
     * @return the widget type.
     */
    public String getOutputTypeName() {
        return outputTypeName;
    }

    /**
     * Creates an empty multiplicity.
     */
    public Multiplicity() {
        super();
    }

    /**
     * @param id the multiplicity identifier.
     * @param name the multiplicity name.
     * @param label the label used to identify the multiplicity in the UI.
     * @param description a brief description of the multiplicity.
     */
    public Multiplicity(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
    }
}
