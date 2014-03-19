package org.iplantc.persistence.dao.data;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.data.IntegrationDatum;

/**
 * @author Dennis Roberts
 */
public interface IntegrationDatumDao extends GenericDao<IntegrationDatum> {

	/**
	 * Searches for an integration datum with the given name and e-mail address.
	 * 
	 * @param name the name of the integrator.
	 * @param email the e-mail address of the integrator.
	 * @return the integration datum or null if no match is found.
	 */
	public IntegrationDatum findByNameAndEmail(String name, String email);
}
