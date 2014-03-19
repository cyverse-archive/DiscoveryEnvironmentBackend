package org.iplantc.workflow.dao.mock;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.TemplateDao;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * Used to access persistent Templates.
 * 
 * @author Dennis Roberts
 */
public class MockTemplateDao extends MockObjectDao<Template> implements TemplateDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public Template findUniqueInstanceByName(String name) {
        List<Template> templates = findByName(name);
        if (templates.size() > 1) {
            throw new WorkflowException("multiple templates found with name: " + name);
        }
        return templates.isEmpty() ? null : templates.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Template> findTemplatesInAnalysis(TransformationActivity analysis) {
        return ListUtils.mapDiscardingNulls(new Lambda<String, Template>() {
            @Override
            public Template call(String templateId) {
                return findById(templateId);
            }
        }, analysis.getTemplateIds());
    }
}
