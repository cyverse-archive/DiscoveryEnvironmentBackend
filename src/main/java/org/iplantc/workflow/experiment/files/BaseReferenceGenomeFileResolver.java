package org.iplantc.workflow.experiment.files;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.refgenomes.ReferenceGenome;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * The base class for resolving reference genomes, sequences and annotations.
 *
 * @author Dennis Roberts
 */
public abstract class BaseReferenceGenomeFileResolver implements FileResolver {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * One or more base filenames to append to the resolved path.
     */
    private List<String> baseFilenames;

    /**
     * @param daoFactory used to obtain data access objects.
     * @param baseFilenames one or more base filenames to append to the resolved path.
     */
    public BaseReferenceGenomeFileResolver(DaoFactory daoFactory, String... baseFilenames) {
        this.daoFactory = daoFactory;
        this.baseFilenames = Arrays.asList(baseFilenames);
    }

    /**
     * Gets the reference genome associated with a UUID.
     * 
     * @param uuid the UUID to search for.
     * @return the associated reference genome.
     * @throws ReferenceGenomeNotFoundException if the UUID isn't associated with any reference genome.
     */
    private ReferenceGenome getReferenceGenome(String uuid) throws ReferenceGenomeNotFoundException {
        ReferenceGenome result = daoFactory.getReferenceGenomeDao().findByUuid(uuid);
        if (result == null) {
            throw new ReferenceGenomeNotFoundException(uuid);
        }
        return result;
    }

    /**
     * Resolves a reference genome.  This method does most of the work for {@link getFileAccessUrl}.
     * 
     * @param uuid the UUID to search for.
     * @return the resolved reference genome path.
     * @throws ReferenceGenomeNotFoundException if the UUID isn't associated with any reference genome.
     */
    private String resolveReferenceGenome(String uuid) throws ReferenceGenomeNotFoundException {
        ReferenceGenome genome = getReferenceGenome(uuid);
        final String basePath = genome.getPath();
        return StringUtils.join(ListUtils.map(new Lambda<String, String>() {
            @Override
            public String call(String arg) {
                return basePath + arg;
            }
        }, baseFilenames), " ");
    }

    /**
     * Obtains a URL used to obtain access to a file. The way that multiple base filenames are handled by this method
     * appears broken to me because it often creates one command-line option with multiple values, which is something
     * that wouldn't appear to be supported by most programs. This is what the existing code did when this class was
     * written, however, so this behavior is going to persist until we know what the code should actually do.
     *
     * @param uuid the UUID used to refer to the reference genome, sequence or annotation.
     * @return the path to the file containing the reference genome, sequence or annotation.
     * @throws ReferenceGenomeNotFoundException if the UUID isn't associated with any reference genome.
     */
    @Override
    public String getFileAccessUrl(String uuid) throws ReferenceGenomeNotFoundException {
        return StringUtils.isBlank(uuid) ? null : resolveReferenceGenome(uuid);
    }
}
