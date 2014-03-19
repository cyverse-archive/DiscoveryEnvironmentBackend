package org.iplantc.workflow.experiment.files;

import org.iplantc.workflow.dao.DaoFactory;

/**
 * Resolves reference sequences.
 * 
 * @author Dennis Roberts
 */
public class ReferenceSequenceResolver extends BaseReferenceGenomeFileResolver {

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public ReferenceSequenceResolver(DaoFactory daoFactory) {
        super(daoFactory, "genome.fas");
    }
}
