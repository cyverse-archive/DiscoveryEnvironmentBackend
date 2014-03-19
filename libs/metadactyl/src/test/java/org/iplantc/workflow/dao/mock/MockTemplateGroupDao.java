package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.TemplateGroupDao;
import org.iplantc.workflow.template.groups.TemplateGroup;

/**
 * Used to access persistent template groups.
 * 
 * @author Dennis Roberts
 */
public class MockTemplateGroupDao extends MockObjectDao<TemplateGroup> implements TemplateGroupDao {

    /**
     * The next internal identifier to assign.
     */
    private long nextHid = 1000;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(TemplateGroup templateGroup) {
        int position = findInSavedObjects(templateGroup);
        if (position < 0) {
            templateGroup.setHid(nextHid++);
            savedObjects.add(templateGroup);
        }
        else {
            savedObjects.set(position, templateGroup);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateGroup findByHid(long hid) {
        for (TemplateGroup group : getSavedObjects()) {
            if (group.getHid() == hid) {
                return group;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateGroup findUniqueInstanceByName(String name) throws WorkflowException {
        List<TemplateGroup> templateGroups = super.findByName(name);
        if (templateGroups.size() > 1) {
            throw new WorkflowException("multiple template groups found with name: " + name);
        }
        return templateGroups.isEmpty() ? null : templateGroups.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TemplateGroup> findTemplateGroupsContainingAnalysis(TransformationActivity analysis) {
        List<TemplateGroup> results = new ArrayList<TemplateGroup>();
        for (TemplateGroup templateGroup : getSavedObjects()) {
            if (templateGroup.directlyContainsAnalysisWithId(analysis.getId())) {
                results.add(templateGroup);
            }
        }
        return results;
    }

    @Override
    public List<TemplateGroup> findTemplateGroupContainingSubgroup(TemplateGroup group) {
         List<TemplateGroup> results = new ArrayList<TemplateGroup>();
        for (TemplateGroup templateGroup : getSavedObjects()) {
            if (templateGroup.getSub_groups().contains(group)) {
                results.add(templateGroup);
            }
        }
        return results;
    }
}
