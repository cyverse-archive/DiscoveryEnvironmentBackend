package org.iplantc.persistence.dto.listing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Contains the information required to represent a deployed component in the analysis listing services.
 * 
 * @author Dennis Roberts
 */
@Entity
@Table(name = "deployed_component_listing")
public class DeployedComponentListing {

    /**
     * The deployed component listing identifier.  We're currently using the row number for the identifier just to
     * satisfy the requirement for an ID field because there's no other single unique field that can be used as an
     * identifier.  This should be okay, however, because this is a read-only DTO.  Just to be safe and to avoid
     * confusion, no getter is provided for this field.
     */
    @Id
    @SuppressWarnings("unused")
    private long id;

    /**
     * The analysis identifier.
     */
    @Column(name = "analysis_id")
    private long analysisId;

    /**
     * The execution order within the analysis.
     */
    @Column(name = "execution_order")
    private int executionOrder;

    /**
     * The internal identifier of the deployed component.
     */
    @Column(name = "deployed_component_hid")
    private long deployedComponentHid;

    /**
     * The external identifier of the deployed component.
     */
    @Column(name = "deployed_component_id")
    private String deployedComponentId;

    /**
     * The name of the deployed component.  For executables, this is the base name of the executable file.
     */
    @Column(name = "name")
    private String name;

    /**
     * The deployed component description.
     */
    @Column(name = "description")
    private String description;

    /**
     * The location of the deployed component.  For executables, this is the path to the directory containing the
     * executable file.
     */
    @Column(name = "location")
    private String location;

    /**
     * The type of the deployed component.  Currently, the acceptable values for this field are "executable" for
     * analysis that are executed within our local Condor cluster, and "fAPI" for analyses that are ultimately
     * submitted to the Foundational API.
     */
    @Column(name = "type")
    private String type;

    /**
     * The version string for the deployed component.
     */
    @Column(name = "version")
    private String version;

    /**
     * Information about the people or organizations that the deployed component can be attributed to.
     */
    @Column(name = "attribution")
    private String attribution;

    /**
     * @return the internal analysis identifier
     */
    public long getAnalysisId() {
        return analysisId;
    }

    /**
     * @return information about the people or organizations that the deployed component can be attributed to
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * @return the internal deployed component identifier
     */
    public long getDeployedComponentHid() {
        return deployedComponentHid;
    }

    /**
     * @return the deployed component identifier
     */
    public String getDeployedComponentId() {
        return deployedComponentId;
    }

    /**
     * @return a brief description of the deployed component.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the execution order of the deployed component within the analysis
     */
    public int getExecutionOrder() {
        return executionOrder;
    }

    /**
     * @return the location of the deployed component
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the name of the deployed component
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type of the deployed component
     */
    public String getType() {
        return type;
    }

    /**
     * @return the version of the deployed component
     */
    public String getVersion() {
        return version;
    }
}
