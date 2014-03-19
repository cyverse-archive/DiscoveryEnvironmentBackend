package org.iplantc.workflow.data;

import org.iplantc.workflow.model.WorkflowElementTest;

/**
 * Unit tests for org.iplantc.workflow.data.InfoType.
 * 
 * @author Dennis Roberts
 */
public class InfoTypeTest extends WorkflowElementTest<InfoType> {

    // The properties used for info type members.
    private static final String ID = "4321423";
    private static final String NAME = "someInfoType";
    private static final String LABEL = "Some Info Type";
    private static final String DESCRIPTION = "Description of some info type.";

    /**
     * {@inheritDoc}
     */
    @Override
    protected InfoType createInstance() {
        return new InfoType(ID, NAME, LABEL, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InfoType createInstance(String id, String name, String label, String description) {
        return new InfoType(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementLabel() {
        return LABEL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getElementDescription() {
        return DESCRIPTION;
    }
}
