package org.iplantc.workflow.integration.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A null heterogeneous registry used to avoid having null checks all over the place.
 * 
 * @author Dennis Roberts
 */
public class NullHeterogeneousRegistry implements HeterogeneousRegistry {

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void add(Class<T> clazz, Serializable token, T object) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T get(Class<T> clazz, Serializable token) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int size(Class<T> clazz) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getRegisteredObjects(Class<T> clazz) {
        return new ArrayList<T>();
    }
}
