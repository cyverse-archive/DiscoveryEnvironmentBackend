package org.iplantc.workflow.dao;

import java.util.Collection;
import java.util.List;

import org.iplantc.persistence.NamedAndUnique;

/**
 * Used to save an object.
 * 
 * @author Dennis Roberts
 * 
 * @param <T> the type of object that is saved.
 */
public interface GenericObjectDao<T extends NamedAndUnique> {

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
    public void deleteById(String id);

    /**
     * Retrieves all objects.
     * 
     * @return the list of objects.
     */
    public List<T> findAll();

    /**
     * Retrieves the object with the given identifier.
     * 
     * @param id the identifier.
     * @return the object.
     */
    public T findById(String id);

    /**
     * Retrieves the list of objects with the given name.
     * 
     * @param name the name of the objects to delete.
     * @return the list of objects.
     */
    public List<T> findByName(String name);
}
