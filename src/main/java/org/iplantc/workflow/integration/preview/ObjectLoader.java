package org.iplantc.workflow.integration.preview;

import java.io.Serializable;

import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to load objects from JSON objects or arrays.
 * 
 * @author Dennis Roberts
 */
public abstract class ObjectLoader {

    /**
     * A registry of workflow elements indexed by name.
     */
    private HeterogeneousRegistry nameRegistry;

    /**
     * A registry of workflow elements indexed by identifier.
     */
    private HeterogeneousRegistry idRegistry;

    /**
     * @param registry the new name registry.
     */
    public void setNameRegistry(HeterogeneousRegistry registry) {
        nameRegistry = registry;
    }

    /**
     * @return the name registry.
     */
    public HeterogeneousRegistry getNameRegistry() {
        return nameRegistry;
    }

    /**
     * @param registry the new identifier registry.
     */
    public void setIdRegistry(HeterogeneousRegistry registry) {
        idRegistry = registry;
    }

    /**
     * @return the identifier registry.
     */
    public HeterogeneousRegistry getIdRegistry() {
        return idRegistry;
    }

    /**
     * Registers an imported object by name.
     * 
     * @param <T> the type of object.
     * @param clazz the class of object.
     * @param name the name of the object.
     * @param object the object.
     */
    protected <T> void registerByName(Class<T> clazz, Serializable name, T object) {
        if (nameRegistry != null) {
            nameRegistry.add(clazz, name, object);
        }
    }

    /**
     * Registers an imported object by identifier.
     * 
     * @param <T> the type of object.
     * @param clazz the class of object.
     * @param id the identifier of the object.
     * @param object the object.
     */
    protected <T> void registerById(Class<T> clazz, Serializable id, T object) {
        if (idRegistry != null) {
            idRegistry.add(clazz, id, object);
        }
    }

    /**
     * Loads a single object using information in a JSON object.
     * 
     * @param json the JSON object.
     * @throws JSONException if the JSON object does not meet the expectations of the importer.
     */
    public abstract void loadObject(JSONObject json) throws JSONException;

    /**
     * Loads a list of objects using information in a JSON array.
     * 
     * @param array the JSON array.
     * @throws JSONException if the JSON object does not meet the expectations of the importer.
     */
    public abstract void loadObjectList(JSONArray array) throws JSONException;
}
