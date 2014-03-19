package org.iplantc.workflow.util;

/**
 * Static utility methods for validating fields.
 *
 * @author Dennis Roberts
 */
public class ValidationUtils {

    /**
     * Prevent instantiation.
     */
    private ValidationUtils() {
    }

    /**
     * Validates the length of a field.
     *
     * @param clazz the class that the field is in.
     * @param name the name of the field.
     * @param value the value of the field.
     * @param maxLen the maximum field length.
     */
    public static void validateFieldLength(Class<?> clazz, String name, String value, int maxLen) {
        if (value != null) {
            int len = value.length();
            if (len > maxLen) {
                throw new FieldLengthValidationException(clazz, name, len, maxLen);
            }
        }
    }
}
