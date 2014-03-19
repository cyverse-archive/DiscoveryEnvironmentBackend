package org.iplantc.workflow.integration;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.json.TitoDeployedComponentMarshaller;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;

/**
 * Used to export existing deployed components in the database to JSON.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentExporter {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * Used to convert deployed components to JSON objects.
     */
    private TitoDeployedComponentMarshaller marshaller;

    /**
     * Used to find deployed components matching the search criteria.
     */
    private DeployedComponentFinder finder;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public DeployedComponentExporter(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
        marshaller = new TitoDeployedComponentMarshaller();
        finder = new DeployedComponentFinder(daoFactory);
    }

    /**
     * Exports data access objects matching the given search criteria.  See {@link DeployedComponentFinder} for
     * information about the search criteria format.
     * 
     * @param criteria the search criteria.
     * @return a JSON object representing the results.
     */
    public JSONObject export(String criteria) {
        JSONObject result = new JSONObject();
        result.put("components", getMatchingDeployedComponents(criteria));
        return result;
    }

    /**
     * Gets the matching deployed components as a list of JSON objects.  This method requires a bit of a conversion
     * because this class uses {@code net.sf.json.JSONObject} whereas {@link TitoDeployedComponentMarshaller} returns
     * {@code org.json.JSONObject}.
     * 
     * @param criteria the search criteria.
     * @return the matching deployed components as a JSON array.
     */
    private JSONArray getMatchingDeployedComponents(String criteria) {
        return JSONArray.fromObject(ListUtils.map(new Lambda<DeployedComponent, JSONObject>() {
            @Override
            public JSONObject call(DeployedComponent arg) {
                return JSONObject.fromObject(marshaller.toJson(arg).toString());
            }
        }, finder.search(criteria)));
    }
}
