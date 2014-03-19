package org.iplantc.workflow.dao.mock;

import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.ValueTypeDao;
import org.iplantc.workflow.model.ValueType;

/**
 * Used to access persistent value types.
 * 
 * @author Dennis Roberts
 */
public class MockValueTypeDao extends MockObjectDao<ValueType> implements ValueTypeDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueType findUniqueInstanceByName(String name) throws WorkflowException {
        ValueType retval = null;
        for (ValueType valueType : getSavedObjects()) {
            if (StringUtils.equals(name, valueType.getName())) {
                retval = valueType;
                break;
            }
        }
        return retval;
    }
}
