package org.iplantc.workflow.marshaler;

import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;

/**
 * Marshals input properties that are associated with files.
 *
 * @author Dennis Roberts
 */
public class FileUiInputPropertyMarshaler extends UiInputPropertyMarshaler {

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public FileUiInputPropertyMarshaler(DaoFactory daoFactory) {
        super(daoFactory);
    }

    /**
     * Gets the name of the property type to use for an input property that is associated with a file.  For files, the
     * property type is associated with the multiplicity of the data object.
     *
     * @param input the input data object.
     * @return the property type name to use.
     */
    @Override
    protected String getPropertyTypeName(DataObject input) {
        return input.getMultiplicity().getTypeName();
    }
}
