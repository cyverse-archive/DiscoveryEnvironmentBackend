package org.iplantc.workflow.dao;

import java.util.List;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.model.Template;

/**
 * Used to access persistent templates.
 * 
 * @author Dennis Roberts
 */
public interface TemplateDao extends GenericObjectDao<Template> {
    
    /**
     * Finds the single template with the given name.
     * 
     * @param name the name of the template.
     * @return the single template or null if a match isn't found.
     */
    public Template findUniqueInstanceByName(String name);

    /**
     * Finds the templates associated with an analysis.
     * 
     * @param analysis the analysis.
     * @return the list of associated templates.
     */
    public List<Template> findTemplatesInAnalysis(TransformationActivity analysis);
}
