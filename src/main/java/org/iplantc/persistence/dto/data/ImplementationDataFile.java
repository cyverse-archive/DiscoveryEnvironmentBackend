package org.iplantc.persistence.dto.data;

/**
 * Models the required functionality for an object that represents a data
 * file.  These files are used to test inputs and outputs of Analyses and 
 * Deployed Components.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public interface ImplementationDataFile {
	/**
	 * Gets if this is an input file or output file.
	 * @return 
	 *	True if input file, false if output file.
	 */
	public boolean isInputFile();
	
	/**
	 * Gets the filename of the file.
	 * @return 
	 *	Filename of the file.
	 */
	public String getFilename();
}
