package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dao.data.DataSourceDao;
import org.iplantc.persistence.dto.data.DataSource;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * @author Dennis Roberts
 */
public class MockDataSourceDao implements DataSourceDao {

    private List<DataSource> savedObjects = new ArrayList<DataSource>();

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource findByName(final String name) {
        return ListUtils.first(new Predicate<DataSource>() {
            @Override
            public Boolean call(DataSource arg) {
                return StringUtils.equals(arg.getName(), name);
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource findByUuid(final String uuid) {
        return ListUtils.first(new Predicate<DataSource>() {
            @Override
            public Boolean call(DataSource arg) {
                return StringUtils.equals(arg.getUuid(), uuid);
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(DataSource object) {
        savedObjects.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(DataSource object) {
        savedObjects.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<DataSource> objects) {
        savedObjects.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(long id) {
        DataSource dataSource = findById(id);
        if (dataSource != null) {
            delete(dataSource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource findById(final long id) {
        return ListUtils.first(new Predicate<DataSource>() {
            @Override
            public Boolean call(DataSource arg) {
                return arg.getId() == id;
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataSource> findAll() {
        return Collections.unmodifiableList(savedObjects);
    }
}
