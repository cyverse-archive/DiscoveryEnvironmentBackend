package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.iplantc.persistence.dao.refgenomes.ReferenceGenomeDao;
import org.iplantc.persistence.dto.refgenomes.ReferenceGenome;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * @author Dennis Roberts
 */
public class MockReferenceGenomeDao implements ReferenceGenomeDao {

    /**
     * the list of reference genomes that have been saved already.
     */
    private List<ReferenceGenome> savedObjects;

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceGenome findByUuid(final String id) {
        return ListUtils.first(new Predicate<ReferenceGenome>() {
            @Override
            public Boolean call(ReferenceGenome arg) {
                return arg.getUuid().equals(id);
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReferenceGenome> list() {
        List<ReferenceGenome> result = new ArrayList<ReferenceGenome>(savedObjects);
        Collections.sort(result, new Comparator<ReferenceGenome>() {
            @Override
            public int compare(ReferenceGenome a, ReferenceGenome b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(ReferenceGenome object) {
        savedObjects.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(ReferenceGenome object) {
        savedObjects.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<ReferenceGenome> objects) {
        savedObjects.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(long id) {
        ReferenceGenome refGenome = findById(id);
        if (refGenome != null) {
            savedObjects.remove(refGenome);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceGenome findById(final long id) {
        return ListUtils.first(new Predicate<ReferenceGenome>() {
            @Override
            public Boolean call(ReferenceGenome arg) {
                return arg.getId() == id;
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReferenceGenome> findAll() {
        return new ArrayList(savedObjects);
    }
}
