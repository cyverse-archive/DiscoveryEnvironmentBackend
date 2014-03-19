package org.iplantc.workflow.integration.json;

import java.util.HashMap;
import java.util.Map;
import org.iplantc.workflow.data.DataObject;

/**
 * Used to convert existing multiplicity names to the corresponding names used by Tito.  At some point, this should
 * probably be added to the database, but this should suffice for now.
 * 
 * @author Dennis Roberts
 */
public class TitoMultiplicityNames {

    /**
     * Maps internal multiplicity names to the ones used by TITO.
     */
    private static final Map<String, String> TITO_MULTIPLICITY_NAMES = new HashMap<String, String>();

    // Initialize the multiplicity name hash.
    static {
        TITO_MULTIPLICITY_NAMES.put("single", "One");
        TITO_MULTIPLICITY_NAMES.put("many", "Many");
        TITO_MULTIPLICITY_NAMES.put("collection", "Folder");
    }

    /**
     * Prevent instantiation.
     */
    private TitoMultiplicityNames() {
    }

    ;

    /**
     * The TITO multiplicity name for the given data object.
     * 
     * @param dataObject the data object.
     * @return the multiplicity name.
     */
    public static String titoMultiplicityName(DataObject dataObject) {
        return TITO_MULTIPLICITY_NAMES.get(dataObject.getMultiplicityName());
    }
}
