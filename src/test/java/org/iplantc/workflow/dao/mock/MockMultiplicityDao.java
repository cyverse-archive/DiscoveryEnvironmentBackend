package org.iplantc.workflow.dao.mock;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.MultiplicityDao;
import org.iplantc.workflow.data.Multiplicity;

/**
 * Used to access persistent multiplicity instances.
 * 
 * @author Dennis Roberts
 */
public class MockMultiplicityDao extends MockObjectDao<Multiplicity> implements MultiplicityDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public Multiplicity findUniqueInstanceByName(String name) throws WorkflowException {
        List<Multiplicity> multiplicities = findByName(name);
        if (multiplicities.size() > 1) {
            throw new WorkflowException("multiple multiplicities with the name \"" + name + "\" found");
        }
        return multiplicities.size() == 0 ? null : multiplicities.get(0);
    }
}
