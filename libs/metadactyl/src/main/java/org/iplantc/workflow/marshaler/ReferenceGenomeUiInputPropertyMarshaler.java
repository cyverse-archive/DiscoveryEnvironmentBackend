package org.iplantc.workflow.marshaler;

import org.iplantc.persistence.dao.refgenomes.ReferenceGenomeDao;
import org.iplantc.persistence.dto.refgenomes.ReferenceGenome;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marshals input properties that are associated with reference genomes.
 *
 * @author Dennis Roberts
 */
public class ReferenceGenomeUiInputPropertyMarshaler extends UiInputPropertyMarshaler {

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public ReferenceGenomeUiInputPropertyMarshaler(DaoFactory daoFactory) {
        super(daoFactory);
    }

    /**
     * Gets the property name to use for an input data object.  For reference genome properties, this is just the
     * empty string.
     *
     * @param input the input data object.
     * @return the property name to use.
     */
    @Override
    protected String getPropertyName(DataObject input) {
        return "";
    }

    /**
     * Gets the default value for a reference genome input property.
     *
     * @param prop the input property.
     * @return the default value.
     */
    @Override
    protected String getDefaultValue(Property prop) {
        return prop.getDefaultValue();
    }
}
