package org.iplantc.workflow.experiment;

import net.sf.json.JSONObject;

import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * Formats a deployed component for submission to one of our job execution frameworks.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentFormatter {

    /**
     * The data access object factory.
     */
    DaoFactory daoFactory;

    /**
     * Initializes a new deployed component formatter with the given DAO factory.
     * 
     * @param daoFactory used to obtain data access objects.
     */
    public DeployedComponentFormatter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * Creates a new deployed component formatter.
     * 
     * @param componentId
     * @return
     */
    public JSONObject formatComponent(String componentId) {
        return marshallDeployedComponent(loadDeployedComponent(componentId));
    }

    /**
     * Marshalls the deployed component to a JSON object.
     * 
     * @param component the deployed component to marshall.
     * @return the marshalled deployed component.
     */
    private JSONObject marshallDeployedComponent(DeployedComponent component) {
        JSONObject json = new JSONObject();
        json.put("name", component.getName());
        json.put("type", component.getType());
        json.put("description", component.getDescription());
        json.put("location", component.getLocation());
        return json;
    }

    /**
     * Loads the deployed component.
     * 
     * @param componentId the deployed component identifier.
     * @return the deployed component.
     */
    private DeployedComponent loadDeployedComponent(String componentId) {
        DeployedComponent component = daoFactory.getDeployedComponentDao().findById(componentId);
        if (component == null) {
            throw new WorkflowException("deployed component " + componentId + " not found");
        }
        return component;
    }
}
