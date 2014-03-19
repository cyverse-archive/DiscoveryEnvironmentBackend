package org.iplantc.workflow.mock;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.iplantc.workflow.model.WorkflowElement;

/**
 * A mock workflow element used to test comparisons to other workflow elements.
 *
 * @author Dennis Roberts
 */
public class MockWorkflowElement extends WorkflowElement {

    /**
     * The default constructor.
     */
    public MockWorkflowElement() {
        super();
    }

    /**
     * Populates the mock workflow element with the given identifier, name, label and description.
     *
     * @param id the workflow element identifier.
     * @param name the workflow element name.
     * @param label the label used to identify the workflow element in the UI.
     * @param description a brief description of the workflow element.
     */
    public MockWorkflowElement(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
    }
}
