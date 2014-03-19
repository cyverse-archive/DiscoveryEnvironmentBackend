package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dao.components.ToolTypeDao;
import org.iplantc.persistence.dto.components.ToolType;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * A mock tool type DAO used for testing.
 * 
 * @author Dennis Roberts
 */
public class MockToolTypeDao implements ToolTypeDao {

    private List<ToolType> savedObjects = new ArrayList<ToolType>();

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolType findByName(final String name) {
        return ListUtils.first(new Predicate<ToolType>() {
            @Override
            public Boolean call(ToolType arg) {
                return StringUtils.equals(arg.getName(), name);
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(ToolType object) {
        savedObjects.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(ToolType object) {
        savedObjects.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<ToolType> objects) {
        savedObjects.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(long id) {
        ToolType toolType = findById(id);
        if (toolType != null) {
            delete(toolType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolType findById(final long id) {
        return ListUtils.first(new Predicate<ToolType>() {
            @Override
            public Boolean call(ToolType arg) {
                return arg.getId() == id;
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ToolType> findAll() {
        return Collections.unmodifiableList(savedObjects);
    }
}
