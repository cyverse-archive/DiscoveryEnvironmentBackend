package org.iplantc.workflow.service;

import net.sf.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.service.dto.pipelines.AnalysisDto;
import org.iplantc.workflow.service.dto.pipelines.AnalysisValidationDto;

/**
 * Services used to support the creation of pipelines.
 * 
 * @author Dennis Roberts
 */
public class PipelineService {

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * @param sessionFactory the Hibernate session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Gets the data objects for an analysis.
     * 
     * @param analysisId the analysis identifier.
     * @return a JSON object representing the analysis, its inputs, and its outputs.
     */
    public String getDataObjectsForAnalysis(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return AnalysisDto.forAnalysisId(analysisId, new HibernateDaoFactory(session)).toString();
            }
        });
    }

    /**
     * Gets the data objects for an analysis.
     * 
     * @param daoFactory used to get data access objects.
     * @param analysisId the analysis identifier.
     * @return a JSON object representing the analysis, its inputs, and its outputs.
     */
    public JSONObject getDataObjectsForAnalysisInternal(DaoFactory daoFactory, String analysisId) {
        return AnalysisDto.forAnalysisId(analysisId, daoFactory).toJson();
    }

    /**
     * Determines whether or not an analysis can be used in a pipeline.
     * 
     * @param analysisId the analysis identifier.
     * @return a JSON object indicating whether or not the analysis can be used.
     */
    public String validateAnalysisForPipelines(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return new AnalysisValidationDto(analysisId, new HibernateDaoFactory(session)).toString();
            }
        });
    }

    /**
     * Determines whether or not an analysis can be used in a pipeline.
     * 
     * @param daoFactory used to obtain data access objects.
     * @param analysisId the analysis identifier.
     * @return a JSON object indicating whether or not the analysis can be used.
     */
    public JSONObject validateAnalysisForPipelinesInternal(DaoFactory daoFactory, String analysisId) {
        return new AnalysisValidationDto(analysisId, daoFactory).toJson();
    }
}
