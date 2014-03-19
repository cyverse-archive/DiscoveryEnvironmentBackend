package org.iplantc.workflow.dao.hibernate;

import org.hibernate.Session;
import org.iplantc.persistence.dao.WorkspaceDao;
import org.iplantc.persistence.dao.components.ToolTypeDao;
import org.iplantc.persistence.dao.data.DataSourceDao;
import org.iplantc.persistence.dao.data.IntegrationDatumDao;
import org.iplantc.persistence.dao.hibernate.HibernateWorkspaceDao;
import org.iplantc.persistence.dao.hibernate.components.HibernateToolTypeDao;
import org.iplantc.persistence.dao.hibernate.data.HibernateDataSourceDao;
import org.iplantc.persistence.dao.hibernate.data.HibernateIntegrationDatumDao;
import org.iplantc.persistence.dao.hibernate.listing.HibernateAnalysisGroupDao;
import org.iplantc.persistence.dao.hibernate.listing.HibernateAnalysisListingDao;
import org.iplantc.persistence.dao.hibernate.listing.HibernateRatingListingDao;
import org.iplantc.persistence.dao.hibernate.refgenomes.HibernateReferenceGenomeDao;
import org.iplantc.persistence.dao.hibernate.user.HibernateUserDao;
import org.iplantc.persistence.dao.listing.AnalysisGroupDao;
import org.iplantc.persistence.dao.listing.AnalysisListingDao;
import org.iplantc.persistence.dao.listing.RatingListingDao;
import org.iplantc.persistence.dao.refgenomes.ReferenceGenomeDao;
import org.iplantc.persistence.dao.user.UserDao;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.DataFormatDao;
import org.iplantc.workflow.dao.DeployedComponentDao;
import org.iplantc.workflow.dao.InfoTypeDao;
import org.iplantc.workflow.dao.MultiplicityDao;
import org.iplantc.workflow.dao.NotificationSetDao;
import org.iplantc.workflow.dao.PropertyDao;
import org.iplantc.workflow.dao.PropertyTypeDao;
import org.iplantc.workflow.dao.RatingDao;
import org.iplantc.workflow.dao.RuleTypeDao;
import org.iplantc.workflow.dao.TemplateDao;
import org.iplantc.workflow.dao.TemplateGroupDao;
import org.iplantc.workflow.dao.TransformationActivityDao;
import org.iplantc.workflow.dao.ValueTypeDao;

/**
 * A factory for generating data access objects.
 *
 * @author Dennis Roberts
 */
public class HibernateDaoFactory implements DaoFactory {

    /**
     * The database session.
     */
    private Session session;

    /**
     * Creates a new data access object factory with the given database session.
     *
     * @param session the database session.
     */
    public HibernateDaoFactory(Session session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataFormatDao getDataFormatDao() {
        return new HibernateDataFormatDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeployedComponentDao getDeployedComponentDao() {
        return new HibernateDeployedComponentDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InfoTypeDao getInfoTypeDao() {
        return new HibernateInfoTypeDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultiplicityDao getMultiplicityDao() {
        return new HibernateMultiplicityDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationSetDao getNotificationSetDao() {
        return new HibernateNotificationSetDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyTypeDao getPropertyTypeDao() {
        return new HibernatePropertyTypeDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleTypeDao getRuleTypeDao() {
        return new HibernateRuleTypeDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateDao getTemplateDao() {
        return new HibernateTemplateDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyDao getPropertyDao() {
        return new HibernatePropertyDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateGroupDao getTemplateGroupDao() {
        return new HibernateTemplateGroupDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisGroupDao getAnalysisGroupDao() {
        return new HibernateAnalysisGroupDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisListingDao getAnalysisListingDao() {
        return new HibernateAnalysisListingDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RatingListingDao getRatingListingDao() {
        return new HibernateRatingListingDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationActivityDao getTransformationActivityDao() {
        return new HibernateTransformationActivityDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueTypeDao getValueTypeDao() {
        return new HibernateValueTypeDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkspaceDao getWorkspaceDao() {
        return new HibernateWorkspaceDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDao getUserDao() {
        return new HibernateUserDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RatingDao getRatingDao() {
        return new HibernateRatingDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceGenomeDao getReferenceGenomeDao() {
        return new HibernateReferenceGenomeDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrationDatumDao getIntegrationDatumDao() {
        return new HibernateIntegrationDatumDao(session);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataSourceDao getDataSourceDao() {
        return new HibernateDataSourceDao(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolTypeDao getToolTypeDao() {
        return new HibernateToolTypeDao(session);
    }
}
