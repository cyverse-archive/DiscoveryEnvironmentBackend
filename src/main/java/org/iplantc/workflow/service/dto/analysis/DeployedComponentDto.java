package org.iplantc.workflow.service.dto.analysis;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.listing.DeployedComponentListing;
import org.iplantc.workflow.service.dto.AbstractDto;
import org.iplantc.workflow.service.dto.JsonField;

/**
 * A data transfer object for a deployed component.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentDto extends AbstractDto {

    /**
     * The deployed component identifier.
     */
    @JsonField(name = "id")
    private String id;

    /**
     * The deployed component name.
     */
    @JsonField(name = "name")
    private String name;

    /**
     * The deployed component description.
     */
    @JsonField(name = "description")
    private String description;

    /**
     * The location of the deployed component.
     */
    @JsonField(name = "location")
    private String location;

    /**
     * The type of the deployed component.
     */
    @JsonField(name = "type")
    private String type;

    /**
     * The deployed component version.
     */
    @JsonField(name = "version")
    private String version;

    /**
     * The deployed component attribution.
     */
    @JsonField(name = "attribution")
    private String attribution;

    /**
     * @return the deployed component attribution.
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * @return the deployed component description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the deployed component identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the location of the deployed component.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the deployed component name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the deployed component type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return the version of the deployed component.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param deployedComponent the listing for the deployed component represented by the DTO.
     */
    public DeployedComponentDto(DeployedComponentListing deployedComponent) {
        id = StringUtils.defaultString(deployedComponent.getDeployedComponentId());
        name = StringUtils.defaultString(deployedComponent.getName());
        description = StringUtils.defaultString(deployedComponent.getDescription());
        location = StringUtils.defaultString(deployedComponent.getLocation());
        type = StringUtils.defaultString(deployedComponent.getType());
        version = StringUtils.defaultString(deployedComponent.getVersion());
        attribution = StringUtils.defaultString(deployedComponent.getAttribution());
    }

    /**
     * @param json the JSON object representing the DTO.
     */
    public DeployedComponentDto(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str the JSON string representing the DTO.
     */
    public DeployedComponentDto(String str) {
        fromString(str);
    }
}
