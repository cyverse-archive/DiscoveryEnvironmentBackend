package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.TransformationActivityDao;
import org.iplantc.persistence.dto.transformation.Transformation;

/**
 * Used to access persistent transformation activities.
 *
 * @author Dennis Roberts
 */
public class MockTransformationActivityDao extends MockObjectDao<TransformationActivity> implements
    TransformationActivityDao
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getTemplateIdsInAnalysis(TransformationActivity analysis) {
        Set<String> templateIds = new HashSet<String>();
        for (TransformationStep step : analysis.getSteps()) {
            Transformation transformation = step.getTransformation();
            if (transformation != null && transformation.getTemplate_id() != null) {
                templateIds.add(transformation.getTemplate_id());
            }
        }
        return templateIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TransformationActivity> getAnalysesReferencingTemplateId(String templateId) {
        List<TransformationActivity> results = new LinkedList<TransformationActivity>();
        for (TransformationActivity analysis : getSavedObjects()) {
            for (TransformationStep step : analysis.getSteps()) {
                Transformation transformation = step.getTransformation();
                if (transformation != null && StringUtils.equals(transformation.getTemplate_id(), templateId)) {
                    results.add(analysis);
                }
            }
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationActivity findUniqueInstanceByName(String name) {
        List<TransformationActivity> analyses = findByName(name);
        if (analyses.size() > 1) {
            throw new WorkflowException("multiple analyses found with name: " + name);
        }
        return analyses.isEmpty() ? null : analyses.get(0);
    }
}
