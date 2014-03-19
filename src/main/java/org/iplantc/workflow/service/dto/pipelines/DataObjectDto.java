package org.iplantc.workflow.service.dto.pipelines;

import net.sf.json.JSONObject;
import org.iplantc.workflow.data.DataObject;
import static org.iplantc.workflow.integration.json.TitoMultiplicityNames.titoMultiplicityName;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object used to describe a data object in a pipeline.
 * 
 * @author Dennis Roberts
 */
public class DataObjectDto extends AbstractDto {

    /**
     * The data object identifier.
     */
    @JsonField(name = "id")
    private String id;

    /**
     * The data object name.
     */
    @JsonField(name = "name")
    private String name;

    /**
     * The multiplicity of the data object.
     */
    @JsonField(name = "multiplicity")
    private String multiplicity;

    /**
     * The command-line order of the data object.
     */
    @JsonField(name = "order")
    private int order;

    /**
     * The info type of files associated with the data object.
     */
    @JsonField(name = "file_info_type")
    private String infoType;

    /**
     * The format of the files associated with the data object.
     */
    @JsonField(name = "format")
    private String format;

    /**
     * The data object description.
     */
    @JsonField(name = "description")
    private String description;

    /**
     * True if the data object is required.
     */
    @JsonField(name = "required")
    private boolean required;

    /**
     * True if the data object should be retained.
     */
    @JsonField(name = "retain")
    private boolean retained;

    /**
     * The option switch that appears on the command line.
     */
    @JsonField(name = "cmdSwitch")
    private String cmdSwitch;

    /**
     * @return the data object identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the data object name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the data object multiplicity.
     */
    public String getMultiplicity() {
        return multiplicity;
    }

    /**
     * @return the data object order.
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the info type of files associated with the data object.
     */
    public String getInfoType() {
        return infoType;
    }

    /**
     * @return the format of files associated with the data object.
     */
    public String getFormat() {
        return format;
    }

    /**
     * @return the data object description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if the data object is required.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @return true if files associated with the data object are retained after job execution.
     */
    public boolean isRetained() {
        return retained;
    }

    /**
     * @return the option switch that appears on the command line.
     */
    public String getCmdSwitch() {
        return cmdSwitch;
    }

    /**
     * @param dataObject the data object represented by this DTO.
     */
    public DataObjectDto(DataObject dataObject) {
        id = dataObject.getId();
        name = dataObject.getName();
        multiplicity = titoMultiplicityName(dataObject);
        order = dataObject.getOrderd();
        infoType = dataObject.getInfoTypeName();
        format = dataObject.getDataFormatName();
        description = dataObject.getDescription();
        required = dataObject.isRequired();
        retained = dataObject.getRetain();
        cmdSwitch = dataObject.getSwitchString();
    }

    /**
     * @param json a JSON object representing the data object DTO.
     */
    public DataObjectDto(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str a JSON string representing the data object DTO.
     */
    public DataObjectDto(String str) {
        fromString(str);
    }

    /**
     * Generates a data object DTO for a data object, returning null if the data object is null.
     * 
     * @param dataObject the data object.
     * @return the DTO or null if the data object was null.
     */
    public static DataObjectDto fromDataObject(DataObject dataObject) {
        return dataObject == null ? null : new DataObjectDto(dataObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataObjectDto other = (DataObjectDto) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.multiplicity == null) ? (other.multiplicity != null) : !this.multiplicity.equals(other.multiplicity)) {
            return false;
        }
        if (this.order != other.order) {
            return false;
        }
        if ((this.infoType == null) ? (other.infoType != null) : !this.infoType.equals(other.infoType)) {
            return false;
        }
        if ((this.format == null) ? (other.format != null) : !this.format.equals(other.format)) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        if (this.required != other.required) {
            return false;
        }
        if (this.retained != other.retained) {
            return false;
        }
        if ((this.cmdSwitch == null) ? (other.cmdSwitch != null) : !this.cmdSwitch.equals(other.cmdSwitch)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 47 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 47 * hash + (this.multiplicity != null ? this.multiplicity.hashCode() : 0);
        hash = 47 * hash + this.order;
        hash = 47 * hash + (this.infoType != null ? this.infoType.hashCode() : 0);
        hash = 47 * hash + (this.format != null ? this.format.hashCode() : 0);
        hash = 47 * hash + (this.description != null ? this.description.hashCode() : 0);
        hash = 47 * hash + (this.required ? 1 : 0);
        hash = 47 * hash + (this.retained ? 1 : 0);
        hash = 47 * hash + (this.cmdSwitch != null ? this.cmdSwitch.hashCode() : 0);
        return hash;
    }
}
