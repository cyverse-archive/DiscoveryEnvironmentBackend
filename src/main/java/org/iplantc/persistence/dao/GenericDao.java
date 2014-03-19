package org.iplantc.persistence.dao;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public interface GenericDao<T> {

    /**
     * Saves the given object.
     * 
     * @param object the object to save.
     */
    public void save(T object);

    /**
     * Deletes the given object.
     * 
     * @param object the object to delete.
     */
    public void delete(T object);

    /**
     * Deletes all objects in the given collection.
     * 
     * @param objects the collection of objects to delete.
     */
    public void deleteAll(Collection<T> objects);

    /**
     * Deletes the object with the given identifier.
     * 
     * @param id the identifier of the object to delete.
     */
    public void deleteById(long id);

    /**
     * Retrieves the object with the given identifier.
     * 
     * @param id the identifier.
     * @return the object.
     */
    public T findById(long id);

    /**
     * Retrieves all objects.
     * 
     * @return the list of objects.
     */
    public List<T> findAll();
}
