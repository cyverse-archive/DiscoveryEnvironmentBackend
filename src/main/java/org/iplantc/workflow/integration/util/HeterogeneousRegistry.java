package org.iplantc.workflow.integration.util;

import java.io.Serializable;
import java.util.Collection;

public interface HeterogeneousRegistry {

    /**
     * Adds an object to the registry.
     * 
     * @param <T> the type of object being registered.
     * @param clazz the type of class being registered.
     * @param token the token used to identify the object.
     * @param object the object being registered.
     */
    public <T> void add(Class<T> clazz, Serializable token, T object);

    /**
     * Gets an object from the registry.
     * 
     * @param <T> the type of object being retrieved.
     * @param clazz the type of class being retrieved.
     * @param token the token used to identify the object.
     * @return the object or null if a match isn't found.
     */
    public <T> T get(Class<T> clazz, Serializable token);

    /**
     * Gets the number of objects of the given type in the registry.
     * 
     * @param <T> the type of object.
     * @param clazz the class object.
     * @return the number of objects of the given type in the registry.
     */
    public <T> int size(Class<T> clazz);

    /**
     * Gets all of the objects of the given type in the registry.
     * 
     * @param <T> the type of object.
     * @param clazz the class of object.
     * @return the collection of objects.
     */
    public <T> Collection<T> getRegisteredObjects(Class<T> clazz);

}