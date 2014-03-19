package org.iplantc.workflow.dao.mock;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.PropertyTypeDao;
import org.iplantc.workflow.model.PropertyType;

/**
 * Used to access persistent property types.
 * 
 * @author Dennis Roberts
 */
public class MockPropertyTypeDao extends MockObjectDao<PropertyType> implements PropertyTypeDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyType findUniqueInstanceByName(String name) throws WorkflowException {
        List<PropertyType> propertyTypes = super.findByName(name);
        if (propertyTypes.size() > 1) {
            throw new WorkflowException("multiple property types found with name: " + name);
        }
        return propertyTypes.size() == 0 ? null : propertyTypes.get(0);
    }
}
