package org.iplantc.workflow.integration;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.model.Template;
import org.iplantc.persistence.dto.transformation.Transformation;

/**
 * Used to generate analyses for individual templates.
 *
 * @author Dennis Roberts
 */
public class AnalysisGenerator {

    /**
     * Generates an analysis for the given template.
     *
     * @param template the template.
     * @return the generated analysis.
     */
    public TransformationActivity generateAnalysis(Template template) {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setId(template.getId());
        analysis.setName(template.getName());
        analysis.setDescription(getAnalysisDescription(template.getDescription()));
        analysis.addStep(stepForTemplate(template));
        analysis.setEditedDate(template.getEditedDate());
        analysis.setIntegrationDate(template.getIntegrationDate());
        return analysis;
    }

    /**
     * Gets a description to use for the analysis. If the template description is present, it's copied to the
     * analysis. Otherwise, it uses a canned description.
     *
     * @param templateDescription the description of the original template.
     * @return the description to use for the analysis.
     */
    private String getAnalysisDescription(String templateDescription) {
        return StringUtils.isEmpty(templateDescription) ? "" : templateDescription;
    }

    /**
     * Generates a transformation step for the given template.
     *
     * @param template the template.
     * @return the generated transformation step.
     */
    private TransformationStep stepForTemplate(Template template) {
        TransformationStep step = new TransformationStep();
        step.setGuid(ImportUtils.generateId());
        step.setName(StringUtils.trimToEmpty(template.getName()));
        step.setDescription(StringUtils.trimToEmpty(template.getDescription()));
        step.setTransformation(transformationForTemplate(template));
        return step;
    }

    /**
     * Generates a transformation for the given template.
     *
     * @param template the template.
     * @return the generated transformation.
     */
    private Transformation transformationForTemplate(Template template) {
        Transformation transformation = new Transformation();
        transformation.setName("");
        transformation.setDescription("");
        transformation.setTemplate_id(template.getId());
        return transformation;
    }
}
