package org.iplantc.workflow.util;

/**
 * A predicate that checks for null values.
 * 
 * @author Dennis Roberts
 */
public class NotNullPredicate<T> implements Predicate<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean call(T arg) {
        return arg != null;
    }
}
