package org.iplantc.workflow.marshaler;

import net.sf.json.util.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.model.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marshals all input properties that aren't handled by more specific marshalers.
 *
 * @author Dennis Roberts
 */
public class UiInputPropertyMarshaler {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public UiInputPropertyMarshaler(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * @return a factory used to obtain data access objects.
     */
    protected DaoFactory getDaoFactory() {
        return daoFactory;
    }

    /**
     * Generates the JSON representing an input property.
     *
     * @param step the transformation step.
     * @param prop the input property.
     * @param input the data object for the input property.
     * @return the JSON representation of the input property.
     * @throws JSONException if a JSON error occurs.
     */
    public JSONObject marshalInputProperty(TransformationStep step, Property prop, DataObject input)
            throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", step.getName() + "_" + input.getId());
        json.put("name", getPropertyName(input));
        json.put("label", StringUtils.isEmpty(input.getLabel()) ? input.getName() : input.getLabel());
        json.put("isVisible", true);
        json.put("type", getPropertyTypeName(input));
        json.put("description", StringUtils.defaultString(input.getDescription()));
        json.put("validator", marshalValidator(input));
        JsonUtils.putIfNotNull(json, "value", getDefaultValue(prop));
        return json;
    }

    /**
     * Gets the property name to use for an input data object.
     *
     * @param input the input data object.
     * @return the property name to use.
     */
    protected String getPropertyName(DataObject input) {
        return input.getSwitchString();
    }

    /**
     * Gets the default value to use for an input property. In the most common case, the default value is always
     * null.
     *
     * @param prop the input property.
     * @return the default value.
     */
    protected String getDefaultValue(Property prop) {
        return null;
    }

    /**
     * Gets the name of the property type to use for an input data object.
     *
     * @param input the input data object.
     * @return the property type name to use.
     */
    protected String getPropertyTypeName(DataObject input) {
        return input.getInfoTypeName();
    }

    /**
     * Generates the JSON representing a validator for an input property.
     *
     * @param input the data object for the input property.
     * @return the JSON representation of the input property validator.
     * @throws JSONException if a JSON error occurs.
ß     */
    protected JSONObject marshalValidator(DataObject input) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", "");
        json.put("label", "");
        json.put("required", input.isRequired());
        JsonUtils.putIfNotNull(json, "rules", marshalValidationRules(input));
        return json;
    }

    /**
     * Generates the JSON representing the list of rules for an input property validator.
     *
     * @param input the data object for the input property.
     * @return the JSON representing the list of rules for the input property validator.
     * @throws JSONException if a JSON error occurs.
     */
    protected JSONArray marshalValidationRules(DataObject input) throws JSONException {
        return null;
    }

    /**
     * Obtains the appropriate UI input property marshaler for a data object.
     *
     * @param daoFactory used by the input property marshaler to obtain data access objects.
     * @param input the data object for the input property.
     * @return the UI input property marshaler to use.
     */
    public static UiInputPropertyMarshaler instance(DaoFactory daoFactory, DataObject input) {
        switch (MarshaledInfoType.forDisplayName(input.getInfoTypeName())) {
            case REFERENCE_GENOME:
            case REFERENCE_SEQUENCE:
            case REFERENCE_ANNOTATION:
                return new ReferenceGenomeUiInputPropertyMarshaler(daoFactory);

            default:
                return new FileUiInputPropertyMarshaler(daoFactory);
        }
    }
}
