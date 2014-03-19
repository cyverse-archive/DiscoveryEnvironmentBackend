package org.iplantc.persistence.dto.data;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.iplantc.persistence.dto.components.DeployedComponent;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
@Entity
@Table(name = "deployed_component_data_files")
public class DeployedComponentDataFile implements Serializable, ImplementationDataFile {
	private long id;
	private String filename;
	private boolean inputFile;
	private DeployedComponent deployedComponent;
	
	public DeployedComponentDataFile() {
		
	}

	@Column(name = "filename", nullable = false)
	@Override
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@SequenceGenerator(name="deployed_component_data_files_id_seq", sequenceName="deployed_component_data_files_id_seq")
	@GeneratedValue(generator="deployed_component_data_files_id_seq")
	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "input_file")
	@Override
	public boolean isInputFile() {
		return inputFile;
	}

	public void setInputFile(boolean inputFile) {
		this.inputFile = inputFile;
	}

	@ManyToOne
	@JoinColumn(insertable = false, updatable = false, name = "deployed_component_id")
	public DeployedComponent getDeployedComponent() {
		return deployedComponent;
	}

	public void setDeployedComponent(DeployedComponent deployedComponent) {
		this.deployedComponent = deployedComponent;
	}
}
