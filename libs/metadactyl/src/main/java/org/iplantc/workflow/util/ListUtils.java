package org.iplantc.workflow.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Some convenient list utilities.  The ideas here borrow heavily from Apache Commons Collections, but they're
 * written with generics and immutability in mind.  I considered using Google Guava, but it only provides some
 * of the features we need.  I also considered using Functional Java, but it requires us to use its list data
 * type.
 *
 * @author Dennis Roberts
 */
public class ListUtils {

    /**
     * Prevent instantiation.
     */
    private ListUtils() {}

    /**
     * Provides an immutable way to filter lists.
     *
     * @param <T> the type of element contained in the list.
     * @param predicate the predicate.
     * @param list the list to filter.
     * @return a list of qualifying elements.
     */
    public static <T> List<T> filter(Predicate<T> predicate, List<T> list) {
        List<T> result = new ArrayList<T>();
        for (T element : list) {
            if (predicate.call(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Finds the first element in a list for which a predicate is true.
     *
     * @param <T> the type of element contained in the list.
     * @param predicate the predicate.
     * @param list the list to search.
     * @return the first qualifying element or null if no element qualifies.
     */
    public static <T> T first(Predicate<T> predicate, List<T> list) {
        for (T element : list) {
            if (predicate.call(element)) {
                return element;
            }
        }
        return null;
    }

    /**
     * Finds the index of the first element in a list for which a predicate is true.
     *
     * @param <T> the type of element contained in the list.
     * @param predicate the predicate.
     * @param list the list to search.
     * @return the index of the first qualifying element or null if no element qualifies.
     */
    public static <T> int firstIndex(Predicate<T> predicate, List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.call(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determines if a predicate is true for any element in a collection.
     *
     * @param <T> the type of element in the collection.
     * @param predicate the predicate.
     * @param coll the collection.
     * @return true if the predicate is true for any element in the collection.
     */
    public static <T> boolean any(Predicate<T> predicate, Collection<T> coll) {
        for (T element : coll) {
            if (predicate.call(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a predicate is true for all elements in a collection.
     * 
     * @param <T> the type of element in the collection.
     * @param predicate the predicate.
     * @param coll the collection.
     * @return true if the predicate is true for all elements in the collection.
     */
    public static <T> boolean all(Predicate<T> predicate, Collection<T> coll) {
        for (T element : coll) {
            if (!predicate.call(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list containing all of the non-null elements that are provided.
     * 
     * @param <T> the type of element in the list.
     * @param elements the elements to place in the list.
     * @return the list of elements.
     */
    public static <T> List<T> asListWithoutNulls(T... elements) {
        List<T> result = new ArrayList<T>();
        for (T element : elements) {
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Maps a source list to a destination list.
     * 
     * @param <I> the source element type.
     * @param <O> the destination element type.
     * @param lambda the function called to map each element.
     * @param source the source list.
     * @return the destination list.
     */
    public static <I, O> List<O> map(Lambda<I, O> lambda, List<I> source) {
        List<O> dest = new ArrayList<O>();
        for (I element : source) {
            dest.add(lambda.call(element));
        }
        return dest;
    }
    
    /**
     * Maps a source list to a destination list.  Any null elements that are produced by the Lambda will not be
     * included in the destination list.
     * 
     * @param <I> the source element type.
     * @param <O> the destination element type.
     * @param lambda the function called to map each element.
     * @param source the source list.
     * @return the destination list.
     */
    public static <I, O> List<O> mapDiscardingNulls(Lambda<I, O> lambda, List<I> source) {
        List<O> dest = new ArrayList<O>();
        for (I sourceElement : source) {
            O destElement = lambda.call(sourceElement);
            if (destElement != null) {
                dest.add(destElement);
            }
        }
        return dest;
    }

    /**
     * Counts the elements in a list that satisfy a predicate.
     * 
     * @param <T> the type of the elements in the list.
     * @param predicate the predicate.
     * @param list the list.
     * @return the number of elements that satisfy the predicate.
     */
    public static <T> int count(Predicate<T> predicate, Collection<T> list) {
        int count = 0;
        for (T element : list) {
            if (predicate.call(element)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a list containing all of the elements in a source list along with one or more additional elements.
     * 
     * @param <T> the type of the elements in the list.
     * @param list the source list.
     * @param elements the additional elements.
     * @return the new list.
     */
    public static <T> List<T> conjoin(List<T> list, T... elements) {
        List<T> result = new ArrayList<T>();
        result.addAll(list);
        result.addAll(Arrays.asList(elements));
        return result;
    }

    /**
     * Joins one or more lists into a single list.
     * 
     * @param <T> the type of the elements in the list.
     * @param lists the source lists.
     * @return a list containing all of the elements in each of the source lists.
     */
    public static <T> List<T> conjoin(List<T>... lists) {
        List<T> result = new ArrayList<T>();
        for (List<T> list : lists) {
            result.addAll(list);
        }
        return result;
    }
    
    /**
     * Joins one or more lists into a single list.
     * 
     * @param <T> the type of the elements in the list.
     * @param lists the source lists.
     * @return a list containing all of the elements in each of the source lists.
     */
    public static <T> List<T> conjoin(List<List<T>> lists) {
        List<T> result = new ArrayList<T>();
        for (List<T> list : lists) {
            result.addAll(list);
        }
        return result;
    }
}
