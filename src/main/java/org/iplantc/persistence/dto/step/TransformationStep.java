package org.iplantc.persistence.dto.step;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.iplantc.persistence.dto.transformation.Transformation;



/**
 * This class defines an element in a transformation task
 * called a transformation step, it contains a set of parameters
 * for a specific type of transformation to be executed.
 * 
 * This class can be thought as a vertex in a DAG.
 * 
 * @author Juan Antonio Raygoza Garay
 *
 */
@Entity
@Table(name = "transformation_steps")
public class TransformationStep implements Serializable {
	private long id;
	protected String guid;
	protected String name;
	protected String description;
	
	Transformation transformation;

	public TransformationStep() {
		
	}
	
	@SequenceGenerator(name="transformation_steps_id_seq", sequenceName="transformation_steps_id_seq")
	@GeneratedValue(generator="transformation_steps_id_seq")
	@Id
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	@Column(name = "name", nullable = true)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "description", nullable = true)
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "guid", nullable = true)
	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "transformation_id")
	public Transformation getTransformation() {
		return transformation;
	}
	
	public void setTransformation(Transformation transformation) {
		this.transformation = transformation;
	}
	
    public void copy(TransformationStep other) {
        name = other.getName();
        description = other.getDescription();
        transformation = other.getTransformation();
    }

    /**
     * Gets the template ID for this transformation step.
     * 
     * @return the template ID or null if there is no associated template.
     */
    @Transient
    public String getTemplateId() {
        return transformation == null ? null : transformation.getTemplate_id();
    }
}
