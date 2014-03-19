
package org.iplantc.workflow.integration.preview;

import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.json.TitoAnalysisUnmarshaller;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to load analyses that are represented by JSON objects into memory.
 * 
 * @author Dennis Roberts
 */
public class AnalysisLoader extends ObjectLoader {

    /**
     * The factory used to generate data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory the factory used go generate data access objects.
     */
    public AnalysisLoader(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadObject(JSONObject json) throws JSONException {
        TitoAnalysisUnmarshaller unmarshaller = new TitoAnalysisUnmarshaller(daoFactory, getNameRegistry());
        if (json != null) {
            loadAnalysis(unmarshaller, json);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadObjectList(JSONArray array) throws JSONException {
        TitoAnalysisUnmarshaller unmarshaller = new TitoAnalysisUnmarshaller(daoFactory, getNameRegistry());
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                loadAnalysis(unmarshaller, json);
            }
        }
    }

    /**
     * Loads an analysis from the given JSON object using the given unmarshaller.
     * 
     * @param unmarshaller used to convert the JSON object to a transformation activity.
     * @param json the JSON object.
     * @throws JSONException if the JSON object doesn't meet the needs of the unmarshaller.
     */
    private void loadAnalysis(TitoAnalysisUnmarshaller unmarshaller, JSONObject json) throws JSONException {
        TransformationActivity analysis = unmarshaller.fromJson(json);
        registerByName(TransformationActivity.class, analysis.getName(), analysis);
        registerById(TransformationActivity.class, analysis.getName(), analysis);
    }
}
