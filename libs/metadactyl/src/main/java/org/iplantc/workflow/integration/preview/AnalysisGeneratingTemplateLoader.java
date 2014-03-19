package org.iplantc.workflow.integration.preview;

import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.AnalysisGenerator;
import org.iplantc.workflow.integration.json.TitoTemplateUnmarshaller;
import org.iplantc.workflow.model.Template;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A template loader that automatically generates analyses for newly imported templates.
 * 
 * @author Dennis Roberts
 */
public class AnalysisGeneratingTemplateLoader extends TemplateLoader {

    /**
     * @param daoFactory the factory used to create data access objects.
     */
    public AnalysisGeneratingTemplateLoader(DaoFactory daoFactory) {
        super(daoFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadTemplate(TitoTemplateUnmarshaller unmarshaller, JSONObject json) throws JSONException {
        Template template = unmarshaller.fromJson(json);
        registerByName(Template.class, template.getName(), template);
        registerById(Template.class, template.getId(), template);
        generateAnalysis(template);
    }

    /**
     * Generates and registers a single-step analysis for the given template.
     * 
     * @param template the template.
     */
    private void generateAnalysis(Template template) {
        TransformationActivity analysis = new AnalysisGenerator().generateAnalysis(template);
        registerByName(TransformationActivity.class, analysis.getName(), analysis);
        registerById(TransformationActivity.class, analysis.getName(), analysis);
    }
}
