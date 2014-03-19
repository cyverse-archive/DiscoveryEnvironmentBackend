package org.iplantc.workflow.service.util;

import java.util.List;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Template;

/**
 * A class that can be used to find unreferenced inputs in an analysis.
 * 
 * @author Dennis Roberts
 */
public class UnreferencedInputFinder extends InputOutputFinder {

    /**
     * @param analysis the analysis.
     * @param daoFactory the data access object factory.
     */
    public UnreferencedInputFinder(TransformationActivity analysis, DaoFactory daoFactory) {
        super(analysis, daoFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<DataObject> getCandidateObjects(Template template) {
        return template.getInputs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isQualifyingDataObject(TransformationStep step, DataObject dataObject) {
        return !getAnalysis().isTargetInMapping(step.getName(), dataObject.getId());
    }
}
