package org.iplantc.persistence.dao.refgenomes;

import java.util.List;
import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.refgenomes.ReferenceGenome;

/**
 * @author Dennis Roberts
 */
public interface ReferenceGenomeDao extends GenericDao<ReferenceGenome> {

    /**
     * Finds a reference genome by its UUID.
     * 
     * @param id the UUID.
     * @return the reference genome.
     */
    public ReferenceGenome findByUuid(String id);

    /**
     * Lists all reference genomes that have not been marked as deleted.
     * 
     * @return the list of reference genomes, ordered by name.
     */
    public List<ReferenceGenome> list();
}
