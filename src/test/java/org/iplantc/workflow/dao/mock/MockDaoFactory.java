package org.iplantc.workflow.dao.mock;

import org.iplantc.persistence.dao.WorkspaceDao;
import org.iplantc.persistence.dao.components.ToolTypeDao;
import org.iplantc.persistence.dao.data.DataSourceDao;
import org.iplantc.persistence.dao.data.IntegrationDatumDao;
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
 * A factory for generating mock data access objects.
 *
 * @author Dennis Roberts
 */
public class MockDaoFactory implements DaoFactory {

    /**
     * The mock data format DAO.
     */
    private MockDataFormatDao dataFormatDao;

    /**
     * The mock deployed component DAO.
     */
    private MockDeployedComponentDao deployedComponentDao;

    /**
     * The mock information type DAO.
     */
    private MockInfoTypeDao infoTypeDao;

    /**
     * The mock multiplicity DAO.
     */
    private MockMultiplicityDao multiplicityDao;

    /**
     * The mock notification set DAO.
     */
    private MockNotificationSetDao notificationSetDao;

    /**
     * The mock property type DAO.
     */
    private MockPropertyTypeDao propertyTypeDao;

    /**
     * The mock rule type DAO.
     */
    private MockRuleTypeDao ruleTypeDao;

    /**
     * The mock template DAO.
     */
    private MockTemplateDao templateDao;

    /**
     * The mock property DAO.
     */
    private MockPropertyDao propertyDao;

    /**
     * The mock template group DAO.
     */
    private MockTemplateGroupDao templateGroupDao;

    /**
     * The mock transformation activity DAO.
     */
    private MockTransformationActivityDao transformationActivityDao;

    /**
     * The mock value type DAO.
     */
    private MockValueTypeDao valueTypeDao;

    /**
     * The mock workspace DAO.
     */
    private MockWorkspaceDao workspaceDao;

    /**
     * The mock user DAO.
     */
    private MockUserDao userDao;

    /**
     * The mock reference genome DAO.
     */
    private MockReferenceGenomeDao referenceGenomeDao;

    /**
     * The mock integration datum DAO.
     */
    private MockIntegrationDatumDao integrationDatumDao;

    /**
     * The mock data source DAO.
     */
    private MockDataSourceDao dataSourceDao;

    /**
     * The mock tool type DAO.
     */
    private MockToolTypeDao toolTypeDao;

    /**
     * @param dataFormatDao the new mock data format DAO.
     */
    public void setMockDataFormatDao(MockDataFormatDao dataFormatDao) {
        this.dataFormatDao = dataFormatDao;
    }

    /**
     * @return the mock data format DAO.
     */
    public MockDataFormatDao getMockDataFormatDao() {
        return dataFormatDao;
    }

    /**
     * @param deployedComponentDao the new mock deployed component DAO.
     */
    public void setMockDeployedComponentDao(MockDeployedComponentDao deployedComponentDao) {
        this.deployedComponentDao = deployedComponentDao;
    }

    /**
     * @return the mock deployed component DAO.
     */
    public MockDeployedComponentDao getMockDeployedComponentDao() {
        return deployedComponentDao;
    }

    /**
     * @param infoTypeDao the new mock info type DAO.
     */
    public void setMockInfoTypeDao(MockInfoTypeDao infoTypeDao) {
        this.infoTypeDao = infoTypeDao;
    }

    /**
     * @return the mock info type DAO.
     */
    public MockInfoTypeDao getMockInfoTypeDao() {
        return infoTypeDao;
    }

    /**
     * @param multiplicityDao the new multiplicity DAO.
     */
    public void setMockMultiplicityDao(MockMultiplicityDao multiplicityDao) {
        this.multiplicityDao = multiplicityDao;
    }

    /**
     * @return the multiplicity DAO.
     */
    public MockMultiplicityDao getMockMultiplicityDao() {
        return multiplicityDao;
    }

    /**
     * @param notificationSetDao the new mock notification set DAO.
     */
    public void setMockNotificationSetDao(MockNotificationSetDao notificationSetDao) {
        this.notificationSetDao = notificationSetDao;
    }

    /**
     * @return the mock notification set DAO.
     */
    public MockNotificationSetDao getMockNotificationSetDao() {
        return notificationSetDao;
    }

    /**
     * @param propertyTypeDao the new mock property type DAO.
     */
    public void setMockPropertyTypeDao(MockPropertyTypeDao propertyTypeDao) {
        this.propertyTypeDao = propertyTypeDao;
    }

    /**
     * @return the mock property type DAO.
     */
    public MockPropertyTypeDao getMockPropertyTypeDao() {
        return propertyTypeDao;
    }

    /**
     * @param ruleTypeDao the new mock rule type DAO.
     */
    public void setMockRuleTypeDao(MockRuleTypeDao ruleTypeDao) {
        this.ruleTypeDao = ruleTypeDao;
    }

    /**
     * @return the mock rule type DAO.
     */
    public MockRuleTypeDao getMockRuleTypeDao() {
        return ruleTypeDao;
    }

    /**
     * @param templateDao the new mock template DAO.
     */
    public void setMockTemplateDao(MockTemplateDao templateDao) {
        this.templateDao = templateDao;
    }

    /**
     * @return the mock template DAO.
     */
    public MockTemplateDao getMockTemplateDao() {
        return templateDao;
    }

    /**
     * @param propertyDao the new mock property DAO.
     */
    public void setMockPropertyDao(MockPropertyDao propertyDao) {
        this.propertyDao = propertyDao;
    }

    /**
     * @return the mock property DAO.
     */
    public MockPropertyDao getMockPropertyDao() {
        return propertyDao;
    }

    /**
     * @param templateGroupDao the new mock template group DAO.
     */
    public void setMockTemplateGroupDao(MockTemplateGroupDao templateGroupDao) {
        this.templateGroupDao = templateGroupDao;
    }

    /**
     * @return the mock template group DAO.
     */
    public MockTemplateGroupDao getMockTemplateGroupDao() {
        return templateGroupDao;
    }

    /**
     * @param transformationActivityDao the new mock transformation activity DAO.
     */
    public void setMockTransformationActivityDao(MockTransformationActivityDao transformationActivityDao) {
        this.transformationActivityDao = transformationActivityDao;
    }

    /**
     * @return the transformation activity DAO.
     */
    public MockTransformationActivityDao getMockTransformationActivityDao() {
        return transformationActivityDao;
    }

    /**
     * @param valueTypeDao the new mock value type DAO.
     */
    public void setMockValueTypeDao(MockValueTypeDao valueTypeDao) {
        this.valueTypeDao = valueTypeDao;
    }

    /**
     * @return the mock value type DAO.
     */
    public MockValueTypeDao getMockValueTypeDao() {
        return valueTypeDao;
    }

    /**
     * @param workspaceDao the new mock workspace DAO.
     */
    public void setMockWorkspaceDao(MockWorkspaceDao workspaceDao) {
        this.workspaceDao = workspaceDao;
    }

    /**
     * @return the mock workspace DAO.
     */
    public MockWorkspaceDao getMockWorkspaceDao() {
        return workspaceDao;
    }

    /**
     * @param userDao the new mock user DAO.
     */
    public void setMockUserDao(MockUserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * @return the mock user DAO.
     */
    public MockUserDao getMockUserDao() {
        return userDao;
    }

    /**
     * @param referenceGenomeDao the new mock reference genome DAO.
     */
    public void setMockReferenceGenomeDao(MockReferenceGenomeDao referenceGenomeDao) {
        this.referenceGenomeDao = referenceGenomeDao;
    }

    /**
     * @return the reference genome DAO.
     */
    public MockReferenceGenomeDao getMockReferenceGenomeDao() {
        return referenceGenomeDao;
    }

    /**
     * @param integrationDatumDao the new mock integration datum DAO.
     */
    public void setMockIntegrationDatumDao(MockIntegrationDatumDao integrationDatumDao) {
        this.integrationDatumDao = integrationDatumDao;
    }

    /**
     * @return the integration datum DAO.
     */
    public MockIntegrationDatumDao getMockIntegrationDatumDao() {
        return integrationDatumDao;
    }

    /**
     * @param dataSourceDao the data source DAO.
     */
    public void setMockDataSourceDao(MockDataSourceDao dataSourceDao) {
        this.dataSourceDao = dataSourceDao;
    }

    /**
     * @return the data source DAO.
     */
    public MockDataSourceDao getMockDataSourceDao() {
        return dataSourceDao;
    }

    /**
     * @return the tool type DAO.
     */
    public MockToolTypeDao getMockToolTypeDao() {
        return toolTypeDao;
    }

    /**
     * Initializes the factory with all empty data access objects.
     */
    public MockDaoFactory() {
        dataFormatDao = new MockDataFormatDao();
        deployedComponentDao = new MockDeployedComponentDao();
        infoTypeDao = new MockInfoTypeDao();
        multiplicityDao = new MockMultiplicityDao();
        notificationSetDao = new MockNotificationSetDao();
        propertyTypeDao = new MockPropertyTypeDao();
        ruleTypeDao = new MockRuleTypeDao();
        templateDao = new MockTemplateDao();
        propertyDao = new MockPropertyDao();
        templateGroupDao = new MockTemplateGroupDao();
        transformationActivityDao = new MockTransformationActivityDao();
        valueTypeDao = new MockValueTypeDao();
        workspaceDao = new MockWorkspaceDao();
        userDao = new MockUserDao();
        referenceGenomeDao = new MockReferenceGenomeDao();
        integrationDatumDao = new MockIntegrationDatumDao();
        dataSourceDao = new MockDataSourceDao();
        toolTypeDao = new MockToolTypeDao();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataFormatDao getDataFormatDao() {
        return dataFormatDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeployedComponentDao getDeployedComponentDao() {
        return deployedComponentDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InfoTypeDao getInfoTypeDao() {
        return infoTypeDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultiplicityDao getMultiplicityDao() {
        return multiplicityDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationSetDao getNotificationSetDao() {
        return notificationSetDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyTypeDao getPropertyTypeDao() {
        return propertyTypeDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleTypeDao getRuleTypeDao() {
        return ruleTypeDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateDao getTemplateDao() {
        return templateDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyDao getPropertyDao() {
        return propertyDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateGroupDao getTemplateGroupDao() {
        return templateGroupDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisGroupDao getAnalysisGroupDao() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisListingDao getAnalysisListingDao() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RatingListingDao getRatingListingDao() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationActivityDao getTransformationActivityDao() {
        return transformationActivityDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueTypeDao getValueTypeDao() {
        return valueTypeDao;
    }

    /**
     * {@inheritDoc}
     *
     * TODO: implement a mock workspace DAO
     */
    @Override
    public WorkspaceDao getWorkspaceDao() {
        return workspaceDao;
    }

    @Override
    public UserDao getUserDao() {
        return userDao;
    }

    @Override
    public RatingDao getRatingDao() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceGenomeDao getReferenceGenomeDao() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrationDatumDao getIntegrationDatumDao() {
        return integrationDatumDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSourceDao getDataSourceDao() {
        return dataSourceDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolTypeDao getToolTypeDao() {
        return toolTypeDao;
    }
}
