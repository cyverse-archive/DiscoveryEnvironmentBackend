package org.iplantc.persistence.dto.components;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Represents the type of a deployed component.
 * 
 * @author Dennis Roberts
 */
@NamedQueries({
        @NamedQuery(name = "ToolType.findById", query = "from ToolType where id = :id"),
        @NamedQuery(name = "ToolType.findByName", query = "from ToolType where name = :name")
})
@Entity
@Table(name = "tool_types")
public class ToolType implements Serializable {

    /**
     * The internal tool type identifier.
     */
    private long id;

    /**
     * The unique tool type name.
     */
    private String name;

    /**
     * The label to use when displaying the tool type in the UI.
     */
    private String label;

    /**
     * A brief description of the tool type.
     */
    private String description;

    /**
     * @return a brief description of the tool type.
     */
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    /**
     * @param description a brief description of the tool type.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the internal tool type identifier.
     */
    @Id
    public long getId() {
        return id;
    }

    /**
     * @param id the internal tool type identifier.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the label to use when displaying the tool type in the UI.
     */
    @Column(name = "label")
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to use when displaying the tool type in the UI.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the unique name of the tool type.
     */
    @Column(name = "name")
    public String getName() {
        return name;
    }

    /**
     * @param name the unique name of the tool type.
     */
    public void setName(String name) {
        this.name = name;
    }
}
