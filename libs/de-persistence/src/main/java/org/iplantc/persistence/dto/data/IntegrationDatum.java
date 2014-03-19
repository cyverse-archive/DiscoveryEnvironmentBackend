package org.iplantc.persistence.dto.data;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "IntegrationDatum.findById", query = "from IntegrationDatum where id = :id"),
        @NamedQuery(name = "IntegrationDatum.findByNameAndEmail",
					query = "from IntegrationDatum where integratorName = :name and integratorEmail = :email")
})
@Table(name = "integration_data")
public class IntegrationDatum implements Serializable {
	private Long id;
	private String integratorName;
	private String integratorEmail;
	
	public IntegrationDatum() {
		
	}

	@SequenceGenerator(name = "integration_data_id_seq", sequenceName = "integration_data_id_seq")
	@GeneratedValue(generator = "integration_data_id_seq")
	@Id()
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "integrator_email", nullable = true)
	public String getIntegratorEmail() {
		return integratorEmail;
	}

	public void setIntegratorEmail(String integratorEmail) {
		this.integratorEmail = integratorEmail;
	}

	@Column(name = "integrator_name", nullable = true)
	public String getIntegratorName() {
		return integratorName;
	}

	public void setIntegratorName(String integratorName) {
		this.integratorName = integratorName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final IntegrationDatum other = (IntegrationDatum) obj;
		if ((this.integratorName == null) ? (other.integratorName != null)
				: !this.integratorName.equals(other.integratorName)) {
			return false;
		}
		if ((this.integratorEmail == null) ? (other.integratorEmail != null)
				: !this.integratorEmail.equals(other.integratorEmail)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 37 * hash + (this.integratorName != null ? this.integratorName.hashCode() : 0);
		hash = 37 * hash + (this.integratorEmail != null ? this.integratorEmail.hashCode() : 0);
		return hash;
	}
}
