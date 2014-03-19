package org.iplantc.workflow.util;

/**
 * A simple interface to simulate anonymous functions.
 *
 * @author Dennis Roberts
 */
public interface Lambda<I, O> {

    /**
     * Call the anonymous function.
     *
     * @param arg the argument.
     * @return the result.
     */
    public O call(I arg);
}
