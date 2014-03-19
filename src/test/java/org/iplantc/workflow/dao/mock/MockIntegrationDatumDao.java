package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dao.data.IntegrationDatumDao;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * @author Dennis Roberts
 */
public class MockIntegrationDatumDao implements IntegrationDatumDao {

    /**
     * The list of saved objects.
     */
    private List<IntegrationDatum> savedObjects;

    /**
     * Default constructor.
     */
    public MockIntegrationDatumDao() {
        savedObjects = new ArrayList<IntegrationDatum>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrationDatum findByNameAndEmail(final String name, final String email) {
        return ListUtils.first(new Predicate<IntegrationDatum>() {
            @Override
            public Boolean call(IntegrationDatum arg) {
                return StringUtils.equals(arg.getIntegratorName(), name)
                        && StringUtils.equals(arg.getIntegratorEmail(), email);
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(IntegrationDatum object) {
        savedObjects.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(IntegrationDatum object) {
        savedObjects.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<IntegrationDatum> objects) {
        savedObjects.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(long id) {
        IntegrationDatum integrationDatum = findById(id);
        if (integrationDatum != null) {
            savedObjects.remove(integrationDatum);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrationDatum findById(final long id) {
        return ListUtils.first(new Predicate<IntegrationDatum>() {
            @Override
            public Boolean call(IntegrationDatum arg) {
                return arg.getId() == id;
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntegrationDatum> findAll() {
        return new ArrayList<IntegrationDatum>(savedObjects);
    }
}
