package org.iplantc.workflow.experiment.files;

import org.iplantc.workflow.dao.DaoFactory;

/**
 * Resolves reference annotations.
 * 
 * @author Dennis Roberts
 */
public class ReferenceAnnotationResolver extends BaseReferenceGenomeFileResolver {

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public ReferenceAnnotationResolver(DaoFactory daoFactory) {
        super(daoFactory, "annotation.gtf");
    }
}
