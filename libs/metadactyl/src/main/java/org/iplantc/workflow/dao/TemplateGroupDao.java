package org.iplantc.workflow.dao;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.template.groups.TemplateGroup;

/**
 * Used to access persistent template groups.
 * 
 * @author Dennis Roberts
 */
public interface TemplateGroupDao extends GenericObjectDao<TemplateGroup> {

    /**
     * Finds the template group with the given Hibernate identifier.
     * 
     * @param hid the Hibernate identifier.
     * @return the template group or null if the template group wasn't found.
     */
    public TemplateGroup findByHid(long hid);

    /**
     * Finds the single template group with the given name.
     * 
     * @param name the template group name.
     * @return the template group or null if no matching template group is found.
     * @throws WorkflowException if more than one matching template group is found.
     */
    public TemplateGroup findUniqueInstanceByName(String name) throws WorkflowException;

    /**
     * Finds all transformation activities that contain the given analysis.
     * 
     * @param analysis the analysis to search for.
     * @return the list of transformation activities that contain the analysis.
     */
    public List<TemplateGroup> findTemplateGroupsContainingAnalysis(TransformationActivity analysis);
    
    /**
     * Finds all TemplateGroups which contain this group as a subgroup.
     * 
     * @param group
     *  Subgroup to match on.
     * @return 
     *  List of groups that contain <code>group</code>.
     */
    public List<TemplateGroup> findTemplateGroupContainingSubgroup(TemplateGroup group);
}
