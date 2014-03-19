package org.iplantc.workflow.integration.preview;

import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.json.TitoTemplateUnmarshaller;
import org.iplantc.workflow.model.Template;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to load templates that are represented by JSON objects into memory.
 * 
 * @author Dennis Roberts
 */
public class TemplateLoader extends ObjectLoader {

    /**
     * The factory used to create data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory the factory used to create data access objects.
     */
    public TemplateLoader(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadObject(JSONObject json) throws JSONException {
        TitoTemplateUnmarshaller unmarshaller = createUnmarshaller();
        if (json != null) {
            loadTemplate(unmarshaller, json);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadObjectList(JSONArray array) throws JSONException {
        TitoTemplateUnmarshaller unmarshaller = createUnmarshaller();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                loadTemplate(unmarshaller, json);
            }
        }
    }

    /**
     * Loads a template from a JSON object.
     * 
     * @param unmarshaller used to convert the JSON object to a template.
     * @param json the JSON object.
     * @throws JSONException if the JSON object doesn't meet the requirements of the unmarshaller.
     */
    protected void loadTemplate(TitoTemplateUnmarshaller unmarshaller, JSONObject json) throws JSONException {
        Template template = unmarshaller.fromJson(json);
        registerByName(Template.class, template.getName(), template);
        registerById(Template.class, template.getId(), template);
    }

    /**
     * Creates the unmarshaller used to convert JSON objects to templates.
     * 
     * @return the unmarshaller.
     */
    private TitoTemplateUnmarshaller createUnmarshaller() {
        return new TitoTemplateUnmarshaller(getNameRegistry(), daoFactory);
    }
}
