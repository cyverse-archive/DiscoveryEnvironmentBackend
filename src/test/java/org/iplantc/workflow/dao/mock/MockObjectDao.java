package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.workflow.dao.GenericObjectDao;

/**
 * A mock object saver used for unit testing.
 * 
 * @author Dennis Roberts
 * 
 * @param <T> the type of object being saved.
 */
public class MockObjectDao<T extends NamedAndUnique> implements GenericObjectDao<T> {

    /**
     * This list of objects that have been saved.
     */
    List<T> savedObjects = new LinkedList<T>();

    /**
     * @return the list of saved objects.
     */
    public List<T> getSavedObjects() {
        return Collections.unmodifiableList(savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(T object) {
        int position = findInSavedObjects(object);
        if (position < 0) {
            savedObjects.add(object);
        }
        else {
            savedObjects.set(position, object);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(T object) {
        savedObjects.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<T> objects) {
        for (T object : objects) {
            savedObjects.remove(object);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(String id) {
        for (int i = 0; i < savedObjects.size(); i++) {
            T object = savedObjects.get(i);
            if (object.getId().equals(id)) {
                savedObjects.remove(i);
                break;
            }
        }
    }

    /**
     * Finds the position of the given object in the list of saved objects.
     * 
     * @param object the object to search for.
     * @return the position or -1 if the object isn't found.
     */
    protected int findInSavedObjects(T object) {
        int result = -1;
        for (int i = 0; i < savedObjects.size(); i++) {
            T currentObject = savedObjects.get(i);
            if (StringUtils.equals(object.getId(), currentObject.getId())) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findAll() {
        return savedObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T findById(String id) {
        T result = null;
        for (T object : savedObjects) {
            if (StringUtils.equals(id, object.getId())) {
                result = object;
                break;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> findByName(String name) {
        List<T> result = new ArrayList<T>();
        for (T object : savedObjects) {
            if (StringUtils.equals(name, object.getName())) {
                result.add(object);
            }
        }
        return result;
    }
}
