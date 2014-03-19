package org.iplantc.persistence.dto.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.iplantc.persistence.PersistenceException;
import org.iplantc.persistence.RepresentableAsJson;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the format of a data object. Some common examples are Nexus, Newick and PDF.
 *
 * @author Dennis Roberts
 */
@NamedQueries({
    @NamedQuery(name = "DataFormat.findByGuid", query = "from DataFormat where guid = :guid"),
    @NamedQuery(name = "DataFormat.findByName", query = "from DataFormat where name = :name")})
@Entity
@Table(name = "data_formats")
public class DataFormat implements RepresentableAsJson {

    /**
     * Specifies the relative display order of the data format.
     */
    private int displayOrder;
    private long id;
    private String guid;
    private String name;
    private String label;

    /**
     * Creates a new empty data format object.
     */
    public DataFormat() {
    }

    /**
     * Creates a new data format with the given ID, name, label and description.
     *
     * @param guid the data format identifier.
     * @param name the data format name.
     * @param label the data format label.
     * @param description the data format description.
     */
    public DataFormat(String guid, String name, String label) {
        this.guid = guid;
        this.name = name;
        this.label = label;
    }

    @Column(name = "guid", nullable = false)
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @SequenceGenerator(name = "data_formats_id_seq", sequenceName = "data_formats_id_seq")
    @GeneratedValue(generator = "data_formats_id_seq")
    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "label", nullable = true)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the display order.
     */
    @Column(name = "display_order", nullable = true)
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * @param displayOrder the new display order.
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataFormat other = (DataFormat) obj;
        if ((this.guid == null) ? (other.guid != null) : !this.guid.equals(other.guid)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + (this.guid != null ? this.guid.hashCode() : 0);
        return hash;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("hid", id);
            json.put("id", guid);
            json.put("name", name);
            json.put("label", label);
            return json;
        } catch (JSONException e) {
            throw new PersistenceException("unable to generate the data format JSON", e);
        }
    }
}
