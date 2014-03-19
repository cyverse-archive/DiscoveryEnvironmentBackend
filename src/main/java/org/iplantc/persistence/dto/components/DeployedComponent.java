package org.iplantc.persistence.dto.components;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.persistence.RepresentableAsJson;
import org.iplantc.persistence.dto.data.DeployedComponentDataFile;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single workflow component (for example, a command-line tool)
 * that has been deployed in a place that is accessible to the discovery
 * environment.
 */
@Entity
@Table(name = "deployed_components")
public class DeployedComponent implements RepresentableAsJson, NamedAndUnique, Serializable {

    /**
     * The component identifier.
     */
    private String id;

    /**
     * The component name.
     */
    private String name;

    /**
     * The component description.
     */
    private String description;

    /**
     * A unique number used to identify the deployed component in the database.
     */
    private long hid;

    /**
     * The location of the deployed component. For executables, this is the path
     * to the executable file on the machines where the jobs are executed.
     */
    private String location;

    /**
     * The tool version number as provided by the tool itself.
     */
    private String version;

    /**
     * A message indicating who the tool can be attributed to.
     */
    private String attribution;

    private IntegrationDatum integrationDatum;

    private Set<DeployedComponentDataFile> deployedComponentDataFiles;

    private ToolType toolType;

    /**
     * @return the component identifier.
     */
    @Column(name = "id", nullable = false)
    @Override
    public String getId() {
        return id;
    }

    /**
     * @param id the new component identifier.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the component name.
     */
    @Column(name = "name", nullable = false)
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the new component name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the component description.
     */
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    /**
     * @param description the new component description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the location of the deployed component.
     */
    @Column(name = "location")
    public String getLocation() {
        return location;
    }

    /**
     * @param location the new location of the deployed component.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the object identifier used in the database.
     */
    @SequenceGenerator(name = "deployed_component_id_seq", sequenceName = "deployed_component_id_seq")
    @GeneratedValue(generator = "deployed_component_id_seq")
    @Id
    public long getHid() {
        return hid;
    }

    /**
     * @param hid the object identifier used in the database.
     */
    public void setHid(long hid) {
        this.hid = hid;
    }

    /**
     * @return the tool version.
     */
    @Column(name = "version")
    public String getVersion() {
        return version;
    }

    /**
     * @param version the new tool version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the attribution.
     */
    @Column(name = "attribution")
    public String getAttribution() {
        return attribution;
    }

    /**
     * @param attribution the new attribution.
     */
    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "integration_data_id", nullable = false)
    public IntegrationDatum getIntegrationDatum() {
        return integrationDatum;
    }

    public void setIntegrationDatum(IntegrationDatum integrationDatum) {
        this.integrationDatum = integrationDatum;
    }

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "deployed_component_id")
    public Set<DeployedComponentDataFile> getDeployedComponentDataFiles() {
        return deployedComponentDataFiles;
    }

    public void setDeployedComponentDataFiles(Set<DeployedComponentDataFile> deployedComponentDataFiles) {
        this.deployedComponentDataFiles = deployedComponentDataFiles;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "tool_type_id")
    public ToolType getToolType() {
        return toolType;
    }

    public void setToolType(ToolType toolType) {
        this.toolType = toolType;
    }

    /**
     * @return the name of the tool type associated with this deployed component.
     */
    @Transient
    public String getType() {
        return toolType == null ? "" : toolType.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("description", description);
            json.put("hid", hid);
            json.put("location", location);
            json.put("type", getType());
            json.put("version", version);
            json.put("attribution", attribution);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}
