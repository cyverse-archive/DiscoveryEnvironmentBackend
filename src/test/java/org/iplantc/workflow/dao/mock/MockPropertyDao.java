package org.iplantc.workflow.dao.mock;

import org.apache.commons.lang.ObjectUtils;
import org.iplantc.workflow.dao.PropertyDao;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Property;

/**
 * A mock property data access object for testing.
 * 
 * @author Dennis Roberts
 */
public class MockPropertyDao extends MockObjectDao<Property> implements PropertyDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getPropertyForDataObject(DataObject dataObject) {
        for (Property property : savedObjects) {
            if (ObjectUtils.equals(dataObject, property.getDataObject())) {
                return property;
            }
        }
        return null;
    }
}
