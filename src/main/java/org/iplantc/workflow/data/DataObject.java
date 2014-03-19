package org.iplantc.workflow.data;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.persistence.dto.data.DataFormat;
import org.iplantc.persistence.dto.data.DataSource;
import org.iplantc.workflow.WorkflowException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.ObjectUtils;

/**
 * A DataObject class defines the data components of a template or a TransformationTask to
 * describe input and output relationship and types.
 *
 *
 * @author Juan Antonio Raygoza Garay
 *
 */
public class DataObject implements NamedAndUnique {

    private long hid;

    private String id;
    private String name;
    private String label;
    private InfoType infoType;
    private DataFormat dataFormat;
    private DataSource dataSource;
    private Multiplicity multiplicity;
    private int orderd;
    private String switchString;
    private String description;
    private boolean required;
    private boolean retain;
    private boolean implicit;

    public boolean getRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public long getHid() {
        return hid;
    }

    public void setHid(long hid) {
        this.hid = hid;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        validateFieldLength(this.getClass(), "id", id, 255);
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateFieldLength(this.getClass(), "name", name, 255);
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        validateFieldLength(this.getClass(), "label", label, 255);
        this.label = label;
    }

    public String getInfoTypeName() {
        return infoType == null ? "" : infoType.getName();
    }

    public String getInfoTypeId() {
        return infoType == null ? "" : infoType.getId();
    }

    public InfoType getInfoType() {
        return infoType;
    }

    public void setInfoType(InfoType infoType) {
        this.infoType = infoType;
    }

    public String getDataFormatName() {
        return dataFormat == null ? "" : dataFormat.getName();
    }

    public String getDataFormatId() {
        return dataFormat == null ? "" : dataFormat.getGuid();
    }

    public DataFormat getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getMultiplicityName() {
        return multiplicity == null ? "" : multiplicity.getName();
    }

    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(Multiplicity multiplicity) {
        this.multiplicity = multiplicity;
    }

    public int getOrderd() {
        return orderd;
    }

    public void setOrderd(int order) {
        this.orderd = order;
    }

    public String getSwitchString() {
        return switchString;
    }

    public void setSwitchString(String switchString) {
        validateFieldLength(this.getClass(), "switchString", switchString, 255);
        this.switchString = switchString;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        validateFieldLength(this.getClass(), "description", description, 255);
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * @return whether this data object is implicit and not included as a cmd line parameter.
     */
    public boolean isImplicit() {
        return implicit;
    }

    /**
     * @param implicit whether this data object is implicit and not included as a cmd line parameter.
     */
    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }

    /**
     * Gets the name of the data source.
     */
    public String getDataSourceName() {
        return dataSource == null ? "" : dataSource.getName();
    }

    /**
     * @return the name of the output type to use.
     */
    public String getOutputTypeName() {
        return multiplicity == null ? "" : multiplicity.getOutputTypeName();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("hid", hid);
            json.put("id", id);
            json.put("name", name);
            json.put("label", label);
            json.put("type", getInfoTypeName());
            json.put("multiplicity", multiplicity);
            json.put("orderd", orderd);
            json.put("switchString", switchString);
            json.put("description", description);
            json.put("required", required);
            json.put("retain", retain);
            json.put("is_implicit", implicit);
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to format JSON", e);
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof DataObject) {
            equals = true;
            DataObject other = (DataObject) o;
            if (!StringUtils.equals(id, other.getId())) {
                equals = false;
            }
            else if (!StringUtils.equals(name, other.getName())) {
                equals = false;
            }
            else if (!StringUtils.equals(label, other.getLabel())) {
                equals = false;
            }
            else if (!ObjectUtils.nullSafeEquals(infoType, other.getInfoType())) {
                equals = false;
            }
            else if (!StringUtils.equals(getMultiplicityName(), other.getMultiplicityName())) {
                equals = false;
            }
            else if (orderd != other.getOrderd()) {
                equals = false;
            }
            else if (!StringUtils.equals(switchString, other.getSwitchString())) {
                equals = false;
            }
            else if (!StringUtils.equals(description, other.getDescription())) {
                equals = false;
            }
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(id)
            + ObjectUtils.nullSafeHashCode(name)
            + ObjectUtils.nullSafeHashCode(label)
            + ObjectUtils.nullSafeHashCode(infoType)
            + ObjectUtils.nullSafeHashCode(multiplicity)
            + orderd
            + ObjectUtils.nullSafeHashCode(switchString)
            + ObjectUtils.nullSafeHashCode(description);
        return hashCode;
    }
}
