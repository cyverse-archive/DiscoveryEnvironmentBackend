package org.iplantc.workflow.integration.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a way to register objects that can be identified using a token.
 * 
 * @author Dennis Roberts
 * 
 * @param <T> the type of object being registered.
 */
public class Registry<T> {

    /**
     * The objects that have been registered so far.
     */
    private Map<Serializable, T> registeredObjects = new HashMap<Serializable, T>();

    /**
     * Adds an object to the registry.
     * 
     * @param token a token used to identify the object in the registry.
     * @param object the object to register.
     */
    public void add(Serializable token, T object) {
        registeredObjects.put(token, object);
    }

    /**
     * @return the size of the registry.
     */
    public int size() {
        return registeredObjects.size();
    }

    /**
     * Retrieves an object from the registry.
     * 
     * @param token a token used to identify the object in the registry.
     * @return the registered object or null if the object isn't found.
     */
    public T get(Serializable token) {
        return registeredObjects.get(token);
    }

    /**
     * Returns the set of registered objects.
     * 
     * @return the set of registered objects.
     */
    public Collection<T> getRegisteredObjects() {
        return registeredObjects.values();
    }
}
