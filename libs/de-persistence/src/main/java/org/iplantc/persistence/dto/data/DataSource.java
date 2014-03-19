package org.iplantc.persistence.dto.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PersistenceException;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.iplantc.persistence.RepresentableAsJson;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the source of a data object. At the time of class creation, sources could include regular files,
 * redirected standard output and redirected standard error output.
 *
 * @author Dennis Roberts
 */
@NamedQueries({
    @NamedQuery(name = "DataSource.findByUuid", query = "from DataSource where uuid = :uuid"),
    @NamedQuery(name = "DataSource.findByName", query = "from DataSource where name = :name")})
@Entity
@Table(name = "data_source")
public class DataSource implements RepresentableAsJson {

    /**
     * The internal identifier (primary key).
     */
    private long id;
    /**
     * The external identifier.
     */
    private String uuid;
    /**
     * The data source name.
     */
    private String name;
    /**
     * The display name to use for the data source.
     */
    private String label;
    /**
     * The data source description.
     */
    private String description;

    /**
     * @return the internal data source identifier.
     */
    @SequenceGenerator(name = "data_source_id_seq", sequenceName = "data_source_id_seq")
    @GeneratedValue(generator = "data_source_id_seq")
    @Id
    public long getId() {
        return id;
    }

    /**
     * @param id the internal data source identifier.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the external data source identifier.
     */
    @Column(name = "uuid", columnDefinition = "bpchar", nullable = false)
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid the internal data source identifier.
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the data source name.
     */
    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    /**
     * @param name the data source name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the data source label.
     */
    @Column(name = "label", nullable = false)
    public String getLabel() {
        return label;
    }

    /**
     * @param label the data source label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the data source description.
     */
    @Column(name = "description", nullable = false)
    public String getDescription() {
        return description;
    }

    /**
     * @param description the data source description.
     */
    public void setDescription(String description) {
        this.description = description;
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
        final DataSource other = (DataSource) obj;
        if ((this.uuid == null) ? (other.uuid != null) : !this.uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("hid", id);
            json.put("id", uuid);
            json.put("name", name);
            json.put("label", label);
            return json;
        }
        catch (JSONException e) {
            throw new PersistenceException("unable to produce the data source JSON", e);
        }
    }
}
