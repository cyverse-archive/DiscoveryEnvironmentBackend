package org.iplantc.workflow.integration;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.json.IdRetentionStrategy;
import org.iplantc.workflow.integration.json.TitoTemplateMarshaller;
import org.iplantc.workflow.model.Template;
import org.json.JSONObject;

/**
 * Used to export existing templates in the database to JSON.
 * 
 * @author Dennis Roberts
 */
public class TemplateExporter {

    /**
     * Used to obtain data access objects.
     */
    private final DaoFactory daoFactory;

    /**
     * Used to marshal templates.
     */
    private final TitoTemplateMarshaller marshaller;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public TemplateExporter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
        marshaller = new TitoTemplateMarshaller(daoFactory, false);
    }

    /**
     * @param daoFactory used to obtain data access objects.
     * @param idRetentionStrategy the ID retention strategy that the marshaler should use.
     */
    public TemplateExporter(DaoFactory daoFactory, IdRetentionStrategy idRetentionStrategy) {
        this.daoFactory = daoFactory;
        marshaller = new TitoTemplateMarshaller(daoFactory, false, idRetentionStrategy);
    }

    /**
     * Exports the template with the given identifier to JSON.
     * 
     * @param templateId the template identifier.
     * @return a JSON object representing the template.
     */
    public JSONObject exportTemplate(String templateId) {
        Template template = daoFactory.getTemplateDao().findById(templateId);
        TransformationActivity app = daoFactory.getTransformationActivityDao().findById(templateId);
        if (template == null) {
            throw new WorkflowException("no template with ID, " + templateId + ", found");
        }
        return marshaller.toJson(template, app);
    }

    /**
     * Exports the template for the analysis with the given identifier.  An exception will be thrown if the analysis
     * can't be found, the analysis has no templates, or the analysis has multiple templates.  Analyses with multiple
     * templates are currently rejected because TITO does not support multi-step analyses.
     * 
     * @param analysisId the analysis identifier.
     * @return a JSON object representing the template.
     */
    public JSONObject exportTemplateForAnalysis(String analysisId) {
        TransformationActivity analysis = getAnalysis(analysisId);
        List<Template> templates = daoFactory.getTemplateDao().findTemplatesInAnalysis(analysis);
        if (templates.isEmpty()) {
            throw new WorkflowException("analysis " + analysisId + " has no templates");
        }
        if (templates.size() > 1) {
            throw new WorkflowException("analysis " + analysisId + " has multiple templates");
        }
        return marshaller.toJson(templates.get(0));
    }

    /**
     * Gets the analysis.
     * 
     * @param analysisId the analysis.
     * @return the analysis.
     */
    private TransformationActivity getAnalysis(String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis with id " + analysisId + " not found");
        }
        return analysis;
    }
}
