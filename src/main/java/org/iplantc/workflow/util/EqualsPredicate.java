package org.iplantc.workflow.util;

import org.apache.commons.lang.ObjectUtils;

/**
 * A predicate that returns true when an object passed to the function matches the object passed in the constructor.
 *
 * @author Dennis Roberts
 */
public class EqualsPredicate<T> implements Predicate<T> {

    /**
     * The object to compare other objects to.
     */
    private T test;

    /**
     * @param test the object to compare other objects to.
     */
    public EqualsPredicate(T test) {
        this.test = test;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean call(T arg) {
        return ObjectUtils.equals(arg, test);
    }
}
