package org.iplantc.workflow.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.RuleTypeDao;
import org.iplantc.workflow.model.RuleType;

/**
 * Used to access persistent rule types in the database.
 * 
 * @author Dennis Roberts
 */
public class HibernateRuleTypeDao extends HibernateGenericObjectDao<RuleType> implements RuleTypeDao {

    /**
     * @param session the database session.
     */
    public HibernateRuleTypeDao(Session session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleType findUniqueInstanceByName(String name) throws WorkflowException {
        List<RuleType> ruleTypes = super.findByName(name);
        if (ruleTypes.size() > 1) {
            throw new WorkflowException("multiple rule types found with name: " + name);
        }
        return ruleTypes.size() == 0 ? null : ruleTypes.get(0);
    }
}
