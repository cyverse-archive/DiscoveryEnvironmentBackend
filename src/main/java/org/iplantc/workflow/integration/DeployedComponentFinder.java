package org.iplantc.workflow.integration;

import java.util.List;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.DeployedComponentDao;
import org.iplantc.workflow.util.ListUtils;

/**
 * Used to look up deployed components in the system.  This class can search for deployed components based on
 * identifier, name, location, or both name and location.  If no search criteria are specified then all deployed
 * components will be returned.  The search criteria are expected to be in the form of a string representation of a
 * JSON object containing the search criteria using the keys, {@code id}, {@code name} and {@code location} for the
 * identifier, name and location, respectively.  As you might expect, passing an empty JSON object to the service
 * will result in all deployed components being matched.
 * 
 * @author Dennis Roberts
 */
public class DeployedComponentFinder {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * @param daoFactory used to obtain data access objects.
     */
    public DeployedComponentFinder(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * Searches for deployed components based on the given search criteria.
     * 
     * @param criteria a JSON string representing the search criteria.
     * @return the list of matching deployed components.
     * @throws IllegalArgumentException if the search criteria are invalid.
     */
    public List<DeployedComponent> search(String criteria) {
        return search(new DeployedComponentSearchCriteria(criteria));
    }

    /**
     * Searches for deployed components based on the given search criteria.
     * 
     * @param criteria the search criteria.
     * @return the list of matching deployed components.
     */
    private List<DeployedComponent> search(DeployedComponentSearchCriteria criteria) {
        DeployedComponentDao dao = daoFactory.getDeployedComponentDao();
        if (criteria.getId() != null) {
            return ListUtils.asListWithoutNulls(dao.findById(criteria.getId()));
        }
        else if (criteria.getName() != null && criteria.getLocation() != null) {
            return ListUtils.asListWithoutNulls(dao.findByNameAndLocation(criteria.getName(), criteria.getLocation()));
        }
        else if (criteria.getName() != null) {
            return dao.findByName(criteria.getName());
        }
        else if (criteria.getLocation() != null) {
            return dao.findByLocation(criteria.getLocation());
        }
        else {
            return dao.findAll();
        }
    }

    /**
     * Represents the search criteria to find existing deployed components.
     */
    public class DeployedComponentSearchCriteria {

        /**
         * The deployed component identifier to search for.
         */
        private String id;
        
        /**
         * The deployed component name to search for.
         */
        private String name;

        /**
         * The deployed component location to search for.
         */
        private String location;

        /**
         * @return the deployed component identifier to search for.
         */
        public String getId() {
            return id;
        }

        /**
         * @return the deployed component name to search for.
         */
        public String getName() {
            return name;
        }

        /**
         * @return the deployed component location to search for.
         */
        public String getLocation() {
            return location;
        }

        /**
         * Creates a new search criteria object based on a JSON string.
         * 
         * @param jsonString the JSON string.
         */
        public DeployedComponentSearchCriteria(String jsonString) {
            try {
                JSONObject json = JSONObject.fromObject(jsonString);
                id = json.optString("id", null);
                name = json.optString("name", null);
                location = json.optString("location", null);
                validateSearchCriteria();
            }
            catch (JSONException e) {
                throw new IllegalArgumentException("invalid deployed component search criteria specifier", e);
            }
        }

        /**
         * Validates the search criteria.  The ID criterion can't be specified along with any other criterion.
         */
        private void validateSearchCriteria() {
            if (id != null && (name != null || location != null)) {
                String msg = "deployed component searches by ID and any other criterion are not currently supported";
                throw new IllegalArgumentException(msg);
            }
        }
    }
}
