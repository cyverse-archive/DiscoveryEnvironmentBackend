package org.iplantc.workflow.dao.mock;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.InfoTypeDao;
import org.iplantc.workflow.data.InfoType;

/**
 * Used to access persistent information types.
 * 
 * @author Dennis Roberts
 */
public class MockInfoTypeDao extends MockObjectDao<InfoType> implements InfoTypeDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public InfoType findUniqueInstanceByName(String name) throws WorkflowException {
        List<InfoType> infoTypes = super.findByName(name);
        if (infoTypes.size() > 1) {
            throw new WorkflowException("multiple information types found with name: " + name);
        }
        return infoTypes.size() == 0 ? null : infoTypes.get(0);
    }
}
