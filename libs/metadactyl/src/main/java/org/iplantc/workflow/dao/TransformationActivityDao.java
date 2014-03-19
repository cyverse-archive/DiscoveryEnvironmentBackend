package org.iplantc.workflow.dao;

import java.util.List;
import java.util.Set;

import org.iplantc.workflow.core.TransformationActivity;

/**
 * Used to access persistent transformation activities.
 *
 * @author Dennis Roberts
 */
public interface TransformationActivityDao extends GenericObjectDao<TransformationActivity> {

    /**
     * Gets the list of template identifiers that are associated with an analysis.
     *
     * @param analysis the analysis to examine.
     * @return the set of template identifiers.
     */
    public Set<String> getTemplateIdsInAnalysis(TransformationActivity analysis);

    /**
     * Counts the number of analyses that reference the given template identifier.
     *
     * @param templateId the template identifier.
     * @return a list of the transformation activities referenced by the template
     */
    public List<TransformationActivity> getAnalysesReferencingTemplateId(String templateId);

    /**
     * Gets the single analysis with the given name.
     *
     * @param name the name of the analysis.
     * @return the analysis or null if a matching analysis isn't found.
     */
    public TransformationActivity findUniqueInstanceByName(String name);
}
