package org.iplantc.persistence.dto.transformation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.iplantc.persistence.dto.step.TransformationStep;

/**
 * This class represents a experiment definition submitted by the user.
 * It contains the difference of what the user input into and the default
 * values defined in the template definition.
 * 
 * @author Juan Antonio Raygoza Garay - iPlant Collaborative
 *
 */
@Entity
@Table(name = "transformations")
public class Transformation implements Serializable {
	/**
	 * Unique identifier for hibernate use.
	 */
	private long id;
	private String name;
	
	/** The id of the template which gave origin to this transformation. */
	private String template_id;
	private String description;
	
	/** Maps the property name to the value entered by the user for each property. */
	private Map<String, String> propertyValues;
	
	private Set<TransformationStep> transformationSteps;
		
	public Transformation() {
		propertyValues = new HashMap<String, String>();
	}
	
	/**
	 * Returns the description of the given transformation.
	 * 
	 * @return the description set by the user
	 */
	@Column(name = "description", nullable = true)
	public String getDescription() {
		return description;
	}
    
	public void setDescription(String description) {
		this.description = description;
	}

	@SequenceGenerator(name = "transformations_id_seq", sequenceName = "transformations_id_seq")
	@GeneratedValue(generator = "transformations_id_seq")
	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "name", nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "template_id", nullable = true)
	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}
	
	public void addPropertyValue(String property, String value) {
		propertyValues.put(property, value);
	}
	
	public boolean containsProperty(String property){
		return propertyValues.keySet().contains(property);
	}
	
	public String getValueForProperty(String property){
		return propertyValues.get(property);
	}

	@ElementCollection
	@MapKeyColumn(name = "property")
	@JoinTable(name = "transformation_values", joinColumns = {@JoinColumn(name = "transformation_id")})
	@Column(name = "value", nullable = false)
	public Map<String, String> getPropertyValues() {
		return propertyValues;
	}

	public void setPropertyValues(Map<String, String> propertyValues) {
		this.propertyValues = propertyValues;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Transformation other = (Transformation) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 53 * hash + (int) (this.id ^ (this.id >>> 32));
		return hash;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "transformation_id")
	public Set<TransformationStep> getTransformationStep() {
		return transformationSteps;
	}

	public void setTransformationStep(Set<TransformationStep> transformationSteps) {
		this.transformationSteps = transformationSteps;
	}
}
