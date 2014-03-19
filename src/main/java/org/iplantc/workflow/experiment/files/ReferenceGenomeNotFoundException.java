package org.iplantc.workflow.experiment.files;

import org.iplantc.workflow.WorkflowException;

/**
 * Thrown when a reference genome can't be resolved.
 * 
 * @author Dennis Roberts
 */
public class ReferenceGenomeNotFoundException extends WorkflowException {

    /**
     * @param uuid the UUID associated with the reference genome.
     */
    public ReferenceGenomeNotFoundException(String uuid) {
        super("reference genome, " + uuid + ", not found");
    }
}
