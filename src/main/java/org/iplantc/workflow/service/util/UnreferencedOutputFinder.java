package org.iplantc.workflow.service.util;

import java.util.List;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Template;

/**
 * A class that can be used to find unreferenced outputs in an analysis.
 * 
 * @author Dennis Roberts
 */
public class UnreferencedOutputFinder extends InputOutputFinder {

    /**
     * @param analysis the analysis.
     * @param daoFactory the data access object factory.
     */
    public UnreferencedOutputFinder(TransformationActivity analysis, DaoFactory daoFactory) {
        super(analysis, daoFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<DataObject> getCandidateObjects(Template template) {
        return template.getOutputs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isQualifyingDataObject(TransformationStep step, DataObject dataObject) {
        return !getAnalysis().isSourceInMapping(step.getName(), dataObject.getId());
    }
}
