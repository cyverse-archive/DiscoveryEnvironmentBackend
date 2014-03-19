package org.iplantc.workflow.experiment.files;

import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * Used to obtain file resolvers for specific info types.
 * 
 * @author Dennis Roberts
 */
public class FileResolverFactory {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public FileResolverFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * Generates a file resolver for the given info type name.
     * 
     * @param infoTypeName the name of the info type associated with a file.
     * @return the info type name or null if the info type isn't resolvable.
     */
    public FileResolver getFileResolver(String infoTypeName) {
        if (StringUtils.equals(infoTypeName, "ReferenceGenome")) {
            return new ReferenceGenomeResolver(daoFactory);
        }
        else if (StringUtils.equals(infoTypeName, "ReferenceSequence")) {
            return new ReferenceSequenceResolver(daoFactory);
        }
        else if (StringUtils.equals(infoTypeName, "ReferenceAnnotation")) {
            return new ReferenceAnnotationResolver(daoFactory);
        }
        else {
            return null;
        }
    }
}
