package org.iplantc.workflow.dao.mock;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DeployedComponentDao;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

public class MockDeployedComponentDao extends MockObjectDao<DeployedComponent> implements DeployedComponentDao {

    /**
     * {@inheritDoc}
     */
    @Override
    public DeployedComponent findByNameAndLocation(String name, String location) {
        DeployedComponent retval = null;
        for (DeployedComponent current : getSavedObjects()) {
            if (StringUtils.equals(name, current.getName()) && StringUtils.equals(location, current.getLocation())) {
                retval = current;
                break;
            }
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeployedComponent findUniqueInstanceByName(String name) {
        List<DeployedComponent> components = findByName(name);
        if (components.size() > 1) {
            throw new WorkflowException("multiple deployed components found with name: " + name);
        }
        return components.isEmpty() ? null : components.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
     public List<DeployedComponent> findByLocation(final String location) {
        return ListUtils.filter(new Predicate<DeployedComponent> () {
            @Override
            public Boolean call(DeployedComponent arg) {
                return StringUtils.equals(location, arg.getLocation());
            }
        }, savedObjects);
    }
}
