package org.iplantc.workflow.marshaler;

import java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * Represents information types that can be marshaled.
 *
 * @author Dennis Roberts
 */
public enum MarshaledInfoType {

    REFERENCE_GENOME("ReferenceGenome"),
    REFERENCE_SEQUENCE("ReferenceSequence"),
    REFERENCE_ANNOTATION("ReferenceAnnotation"),
    DEFAULT("Default");

    /**
     * The display name for the marshaled information type.
     */
    private final String display;

    /**
     * @return the display name for the marshaled information type.
     */
    public String getDisplayName() {
        return display;
    }

    /**
     * @param display the display name for the marshaled information type.
     */
    MarshaledInfoType(String display) {
        this.display = display;
    }

    /**
     * Returns the {@code MarshaledInfoType} for the given display name.  If a matching instance can't be found then
     * the default instance is returned.
     *
     * @param display the display name to search for.
     * @return the matching instance or {@code MarshaledInfoType.DEFAULT} if a matching instance isn't found.
     */
    public static MarshaledInfoType forDisplayName(final String display) {
        MarshaledInfoType result = ListUtils.first(new Predicate<MarshaledInfoType>() {
            @Override
            public Boolean call(MarshaledInfoType arg) {
                return StringUtils.equals(display, arg.getDisplayName());
            }
        }, Arrays.asList(MarshaledInfoType.values()));
        return result == null ? DEFAULT : result;
    }
}
