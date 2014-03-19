package org.iplantc.workflow.integration;

import static org.iplantc.workflow.integration.util.AnalysisImportUtils.getDate;

import net.sf.json.JSONObject;
import org.iplantc.workflow.AnalysisNotFoundException;
import org.iplantc.workflow.MissingRequiredFieldException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * Provides a way to update the information in an analysis without updating any of the components within the analysis.
 *
 * @author Dennis Roberts
 */
public class AnalysisUpdater {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public AnalysisUpdater(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * @param jsonString a JSON string representing the object used to update the analysis.
     */
    public void updateAnalysis(String jsonString) {
        updateAnalysis(JSONObject.fromObject(jsonString));
    }

    /**
     * Updates the analysis.
     * 
     * @param json the JSON object used to update the analysis.
     */
    public void updateAnalysis(JSONObject json) {
        TransformationActivity analysis = loadAnalysis(json.optString("id", null));
        analysis.setName(json.optString("name", analysis.getName()));
        analysis.setDescription(json.optString("description", analysis.getDescription()));
        analysis.setEditedDate(getDate(json.optString("edited_date"), analysis.getEditedDate()));
        analysis.setIntegrationDate(getDate(json.optString("published_date"), analysis.getIntegrationDate()));
        daoFactory.getTransformationActivityDao().save(analysis);
    }

    /**
     * Loads an analysis from the database, throwing an exception if the analysis can't be loaded.
     * 
     * @param id the analysis identifier.
     * @return the analysis.
     * @throws MissingRequiredFieldException if the analysis ID wasn't provided.
     * @throws AnalysisNotFoundException if an analysis with the given ID can't be found.
     */
    private TransformationActivity loadAnalysis(String id) {
        if (id == null) {
            throw new MissingRequiredFieldException("id");
        }
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(id);
        if (analysis == null) {
            throw new AnalysisNotFoundException(id);
        }
        return analysis;
    }
}
