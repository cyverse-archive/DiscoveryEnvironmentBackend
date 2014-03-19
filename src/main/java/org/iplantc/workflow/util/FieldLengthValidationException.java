package org.iplantc.workflow.util;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when a field exceeds its maximum allowable length.
 *
 * @author Dennis Roberts
 */
public class FieldLengthValidationException extends WorkflowException {

    /**
     * @param clazz the class that the field is in.
     * @param fieldName the name of the field.
     * @param len the actual field length.
     * @param maxLen the maximum field length.
     */
    public FieldLengthValidationException(Class<?> clazz, String fieldName, int len, int maxLen) {
        super("maximum allowable field length exceeded for field '" + fieldName + "' in class '"
                + clazz.getSimpleName() + "'; actual length: " + len + "; maximum length: " + maxLen);
    }
}
