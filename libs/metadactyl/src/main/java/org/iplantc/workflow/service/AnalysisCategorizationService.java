package org.iplantc.workflow.service;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.integration.AnalysisCategorizer;
import org.iplantc.workflow.service.dto.AnalysisCategoryList;
import org.iplantc.workflow.service.dto.FailedCategorizationList;
import org.iplantc.workflow.service.dto.FavoriteUpdateRequest;
import org.iplantc.workflow.service.dto.ServiceStatus;

/**
 * Allows analyses to be categorized.
 * 
 * @author Dennis Roberts
 */
public class AnalysisCategorizationService {

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * The index of the development analysis group in the user's workspace.
     */
    private int devAnalysisGroupIndex;

    /**
     * The index of the favorites analysis group in the user's workspace.
     */
    private int favoritesAnalysisGroupIndex;

    /**
     * Used to initialize the user's workspace.
     */
    private WorkspaceInitializer workspaceInitializer;

    /**
     * @param devAnalysisGroupIndex the development analysis group index.
     */
    public void setDevAnalysisGroupIndex(int devAnalysisGroupIndex) {
        this.devAnalysisGroupIndex = devAnalysisGroupIndex;
    }

    /**
     * @param favoritesAnalysisGroupIndex the favorites analysis group index.
     */
    public void setFavoritesAnalysisGroupIndex(int favoritesAnalysisGroupIndex) {
        this.favoritesAnalysisGroupIndex = favoritesAnalysisGroupIndex;
    }

    /**
     * @param sessionFactory the Hibernate session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param workspaceInitializer used to initialize the user's workspace.
     */
    public void setWorkspaceInitializer(WorkspaceInitializer workspaceInitializer) {
        this.workspaceInitializer = workspaceInitializer;
    }

    /**
     * Either adds an analysis to or removes an analysis from the user's favorites group.
     * 
     * @param requestBody the service request body.
     * @return a success indicator.
     */
    public String updateFavorite(String requestBody) {
        final FavoriteUpdateRequest request = new FavoriteUpdateRequest(requestBody);
        new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<Void>() {
            @Override
            public Void perform(Session session) {
                createAnalysisCategorizer(session).updateFavorite(request);
                return null;
            }
        });
        return ServiceStatus.SUCCESS.toString();
    }

    /**
     * Gets the current public analysis categories.
     * 
     * @param analysisSet a string used to specify which analyses to include ("all" or "public").
     * @return a JSON object indicating the categories for each public analysis.
     */
    public String getAnalysisCategories(final String analysisSet) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return createAnalysisCategorizer(session).getAnalysisCategories(analysisSet).toString();
            }
        });
    }

    /**
     * Categorizes a set of analyses.
     * 
     * @param requestBody the body of the request.
     * @return a list of failed categorizations.
     */
    public String categorizeAnalyses(String requestBody) {
        final AnalysisCategoryList categories = new AnalysisCategoryList(requestBody);
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<FailedCategorizationList>() {
            @Override
            public FailedCategorizationList perform(Session session) {
                return createAnalysisCategorizer(session).categorizeAnalyses(categories);
            }
        }).toString();
    }

    /**
     * Creates an analysis categorizer for a Hibernate session.
     * 
     * @param session the Hibernate session.
     * @return the analysis categorizer.
     */
    private AnalysisCategorizer createAnalysisCategorizer(Session session) {
        DaoFactory daoFactory = new HibernateDaoFactory(session);
        return new AnalysisCategorizer(daoFactory, devAnalysisGroupIndex, favoritesAnalysisGroupIndex,
                workspaceInitializer);
    }
}
