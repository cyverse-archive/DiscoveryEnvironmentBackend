package org.iplantc.workflow.dao.mock;

import java.util.List;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.RuleTypeDao;
import org.iplantc.workflow.model.RuleType;

/**
 * Used to access persistent rule types.
 * 
 * @author Dennis Roberts
 */
public class MockRuleTypeDao extends MockObjectDao<RuleType> implements RuleTypeDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleType findUniqueInstanceByName(String name) throws WorkflowException {
        List<RuleType> ruleTypes = findByName(name);
        if (ruleTypes.size() > 1) {
            throw new WorkflowException("multiple rule types found with name: " + name);
        }
        return ruleTypes.size() == 0 ? null : ruleTypes.get(0);
    }
}
