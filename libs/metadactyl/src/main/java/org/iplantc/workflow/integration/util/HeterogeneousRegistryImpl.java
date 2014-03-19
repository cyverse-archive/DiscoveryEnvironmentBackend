package org.iplantc.workflow.integration.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A generalized registry that can be used to map objects to
 * 
 * @author dennis
 */
public class HeterogeneousRegistryImpl implements HeterogeneousRegistry {

    /**
     * The map used to implement the registry.
     */
    Map<Class<?>, Registry<?>> registryMap = new HashMap<Class<?>, Registry<?>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void add(Class<T> clazz, Serializable token, T object) {
        Registry<T> registry = (Registry<T>) getRegistry(clazz);
        registry.add(token, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T get(Class<T> clazz, Serializable token) {
        Registry<T> registry = (Registry<T>) getRegistry(clazz);
        return registry.get(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int size(Class<T> clazz) {
        Registry<T> registry = (Registry<T>) getRegistry(clazz);
        return registry.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getRegisteredObjects(Class<T> clazz) {
        Registry<T> registry = (Registry<T>) getRegistry(clazz);
        return registry.getRegisteredObjects();
    }

    /**
     * Gets the registry for the given type of class.
     * 
     * @param <T> the type of objects stored in the registry.
     * @param clazz the class representing the type
     * @return the (possibly new) registry for the class.
     */
    @SuppressWarnings("unchecked")
    private <T> Registry<T> getRegistry(Class<T> clazz) {
        Registry<T> registry = (Registry<T>) registryMap.get(clazz);
        if (registry == null) {
            registry = new Registry<T>();
            registryMap.put(clazz, registry);
        }
        return registry;
    }
}
