package org.iplantc.workflow.experiment.files;

import org.iplantc.workflow.dao.DaoFactory;

/**
 * Resolves reference genomes.
 *
 * @author Dennis Roberts
 */
public class ReferenceGenomeResolver extends BaseReferenceGenomeFileResolver {

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public ReferenceGenomeResolver(DaoFactory daoFactory) {
        super(daoFactory, "annotation.gtf", "genome.fas");
    }
}
