package org.iplantc.workflow.service.util;

import java.util.List;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * A base class that can be used to find inputs or outputs for an analysis that satisfy certain conditions.
 * 
 * @author Dennis Roberts
 */
public abstract class InputOutputFinder {

    /**
     * The analysis.
     */
    private TransformationActivity analysis;

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @return the analysis.
     */
    public TransformationActivity getAnalysis() {
        return analysis;
    }

    /**
     * @return the data access object factory.
     */
    public DaoFactory getDaoFactory() {
        return daoFactory;
    }

    /**
     * @param analysis the analysis.
     * @param daoFactory the data access object factory.
     */
    public InputOutputFinder(TransformationActivity analysis, DaoFactory daoFactory) {
        this.analysis = analysis;
        this.daoFactory = daoFactory;
    }

    /**
     * Finds the data objects in the analysis that satisfy certain conditions.  The specific conditions are defined
     * by concrete subclasses.
     * 
     * @return the list of data objects.
     */
    public List<DataObject> findDataObjects() {
        return ListUtils.conjoin(ListUtils.map(new Lambda<TransformationStep, List<DataObject>>() {
            @Override
            public List<DataObject> call(TransformationStep arg) {
                return findQualifyingDataObjectsInStep(arg);
            }
        }, analysis.getSteps()));
    }

    /**
     * Finds qualifying data objects in a single transformation step.
     * 
     * @param step the transformation step.
     * @return the list of data objects.
     */
    private List<DataObject> findQualifyingDataObjectsInStep(final TransformationStep step) {
        Template template = findTemplate(step.getTemplateId());
        return ListUtils.filter(new Predicate<DataObject>() {
            @Override
            public Boolean call(DataObject arg) {
                return isQualifyingDataObject(step, arg);
            }
        }, getCandidateObjects(template));
    }

    /**
     * Gets a list of candidate data objects in a template.
     * 
     * @param template the template.
     * @return the list of candidate objects.
     */
    protected abstract List<DataObject> getCandidateObjects(Template template);

    /**
     * Determines whether or not a data object qualifies for inclusion in the list of data objects.
     * 
     * @param step the transformation step.
     * @param dataObject the data object.
     * @return true if the data object qualifies.
     */
    protected abstract boolean isQualifyingDataObject(TransformationStep step, DataObject dataObject);

    /**
     * Finds the template with the given identifier, throwing a WorkflowException if the template can't be found.
     * 
     * @param templateId the template identifier.
     * @return the template.
     */
    private Template findTemplate(String templateId) {
        Template template = daoFactory.getTemplateDao().findById(templateId);
        if (template == null) {
            throw new WorkflowException("template, " + templateId + ", not found");
        }
        return template;
    }
}
