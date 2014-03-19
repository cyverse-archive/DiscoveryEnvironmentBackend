package org.iplantc.persistence;

/**
 * Indicates that an error occurred in the persistence framework.
 * 
 * @author Dennis Roberts
 */
public class PersistenceException extends RuntimeException {

    /**
     * @param cause the cause of this exception.
     */
    public PersistenceException(Throwable cause) {
        super(cause);
    }

    /**
     * @param msg the error detail message.
     * @param cause the cause of this exception.
     */
    public PersistenceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * @param msg the error detail message.
     */
    public PersistenceException(String msg) {
        super(msg);
    }

    /**
     * The default constructor.
     */
    public PersistenceException() {
    }
}
