package org.iplantc.persistence.dto.refgenomes;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import org.iplantc.persistence.dto.user.User;

/**
 * Represents a reference genome that is accessible to Condor jobs in the DE.
 * 
 * @author Dennis Roberts
 */
@NamedQueries({
    @NamedQuery(name = "ReferenceGenome.findById", query = "from ReferenceGenome where id = :id"),
    @NamedQuery(name = "ReferenceGenome.findByUuid", query = "from ReferenceGenome where uuid = :id"),
    @NamedQuery(name = "ReferenceGenome.list", query = "from ReferenceGenome where deleted is false order by name")
})
@Entity
@Table(name = "genome_reference")
public class ReferenceGenome implements Serializable {

    /**
     * The internal identifier used in the database.
     */
    @Id
    private long id;

    /**
     * The external identifier used by other services.
     */
    @Column(name = "uuid", columnDefinition = "bpchar")
    private String uuid;

    /**
     * The name of the reference genome.
     */
    @Column(name = "name")
    private String name;

    /**
     * The path to the reference genome.
     */
    @Column(name = "path")
    private String path;

    /**
     * True if the reference genome has been deleted.
     */
    @Column(name = "deleted")
    private boolean deleted;

    /**
     * The user who first entered the reference genome.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * The date the reference genome was entered.
     */
    @Column(name = "created_on")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdOn;

    /**
     * The user who most recently modified the reference genome.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "last_modified_by")
    private User lastModfifiedBy;

    /**
     * The date the reference genome was last modified.
     */
    @Column(name = "last_modified_on")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastModifiedOn;

    /**
     * @return the user who entered the reference genome.
     */
    public User getCreatedBy() {
        return createdBy;
    }

    /**
     * @return the date that the reference genome was entered.
     */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
     * @return true if the reference genome has been marked as deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @return the internal reference genome identifier.
     */
    public long getId() {
        return id;
    }

    /**
     * @return the user who most recently modified the reference genome entry.
     */
    public User getLastModfifiedBy() {
        return lastModfifiedBy;
    }

    /**
     * @return the date the reference genome entry was most recently modified.
     */
    public Date getLastModifiedOn() {
        return lastModifiedOn;
    }

    /**
     * @return the name of the reference genome.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the path to the reference genome files.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the external identifiers used by other services to reference the genomes.
     */
    public String getUuid() {
        return uuid;
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
        final ReferenceGenome other = (ReferenceGenome) obj;
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
        int hash = 5;
        hash = 37 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ReferenceGenome{" + "id=" + id + ", uuid=" + uuid + ", name=" + name + ", path=" + path + ", deleted="
                + deleted + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", lastModfifiedBy="
                + lastModfifiedBy + ", lastModifiedOn=" + lastModifiedOn + '}';
    }
}
