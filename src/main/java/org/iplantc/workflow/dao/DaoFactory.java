package org.iplantc.workflow.dao;

import org.iplantc.persistence.dao.WorkspaceDao;
import org.iplantc.persistence.dao.components.ToolTypeDao;
import org.iplantc.persistence.dao.data.DataSourceDao;
import org.iplantc.persistence.dao.data.IntegrationDatumDao;
import org.iplantc.persistence.dao.listing.AnalysisGroupDao;
import org.iplantc.persistence.dao.listing.AnalysisListingDao;
import org.iplantc.persistence.dao.listing.RatingListingDao;
import org.iplantc.persistence.dao.refgenomes.ReferenceGenomeDao;
import org.iplantc.persistence.dao.user.UserDao;

/**
 * A factory for generating data access objects.
 *
 * @author Dennis Roberts
 */
public interface DaoFactory {

    /**
     * Creates and returns a data access object for data formats.
     *
     * @return the new data access object.
     */
    public DataFormatDao getDataFormatDao();

    /**
     * Creates and returns a data access object for deployed components.
     *
     * @return the new data access object.
     */
    public DeployedComponentDao getDeployedComponentDao();

    /**
     * Creates and returns a data access object for info types.
     *
     * @return the new data access object.
     */
    public InfoTypeDao getInfoTypeDao();

    /**
     * Creates and returns a data access object for multiplicities.
     *
     * @return the new data access object.
     */
    public MultiplicityDao getMultiplicityDao();

    /**
     * Creates and returns a data access object for notification sets.
     *
     * @return the new data access object.
     */
    public NotificationSetDao getNotificationSetDao();

    /**
     * Creates and returns a data access object for property types.
     *
     * @return the new data access object.
     */
    public PropertyTypeDao getPropertyTypeDao();

    /**
     * Creates and returns a data access object for rule types.
     *
     * @return the new data access object.
     */
    public RuleTypeDao getRuleTypeDao();

    /**
     * Creates and returns a data access object for templates.
     *
     * @return the new data access object.
     */
    public TemplateDao getTemplateDao();

    /**
     * Creates and returns a data access object for properties.
     *
     * @return the new data access object.
     */
    public PropertyDao getPropertyDao();

    /**
     * Creates and returns a data access object for template groups.
     *
     * @return the new data access object.
     */
    public TemplateGroupDao getTemplateGroupDao();

    /**
     * Creates and returns a data access object for analysis groups.
     *
     * @return the new data access object.
     */
    public AnalysisGroupDao getAnalysisGroupDao();

    /**
     * Creates and returns a data access object for analysis listings.
     *
     * @return the new data access object.
     */
    public AnalysisListingDao getAnalysisListingDao();

    /**
     * Creates and returns a data access object for analysis rating listings.
     *
     * @return the new data access object.
     */
    public RatingListingDao getRatingListingDao();

    /**
     * Creates and returns a data access object for transformation activities.
     *
     * @return the new data access object.
     */
    public TransformationActivityDao getTransformationActivityDao();

    /**
     * Creates and returns a data access object for value types.
     *
     * @return the new data access object.
     */
    public ValueTypeDao getValueTypeDao();

    /**
     * Creates and returns a data access object for workspaces.
     *
     * @return the new data access object.
     */
    public WorkspaceDao getWorkspaceDao();

    /**
     * Gets the UserDao.
     */
    public UserDao getUserDao();

    /**
     * Gets the current RatingDao implementation.
     *
     * @return
     */
    public RatingDao getRatingDao();

    /**
     * Creates and returns a data access object for reference genomes.
     *
     * @return the new data access object.
     */
    public ReferenceGenomeDao getReferenceGenomeDao();

    /**
     * Creates and returns a data access object for integration data.
     *
     * @return the new data access object.
     */
    public IntegrationDatumDao getIntegrationDatumDao();

    /**
     * Creates and returns a data access object for data sources.
     * 
     * @return the new data access object.
     */
    public DataSourceDao getDataSourceDao();

    /**
     * Creates and returns a data access object for tool types.
     * 
     * @return the new data access object.
     */
    public ToolTypeDao getToolTypeDao();
}
