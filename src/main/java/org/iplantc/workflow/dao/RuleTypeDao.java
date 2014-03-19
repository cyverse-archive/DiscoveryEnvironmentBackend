package org.iplantc.workflow.dao;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.model.RuleType;

/**
 * Used to access persistent rule types.
 * 
 * @author Dennis Roberts
 */
public interface RuleTypeDao extends GenericObjectDao<RuleType> {

    /**
     * Finds the single instance of this class with the given name.
     * 
     * @param name the rule type name.
     * @return the rule type.
     */
    public RuleType findUniqueInstanceByName(String name) throws WorkflowException;
}
