package org.iplantc.persistence;

/**
 * Represents a named object with a unique identifier.
 * 
 * @author Dennis Roberts
 */
public interface NamedAndUnique {

	/**
	 * @return the object identifier.
	 */
	public String getId();

	/**
	 * @return the object name.
	 */
	public String getName();
}
