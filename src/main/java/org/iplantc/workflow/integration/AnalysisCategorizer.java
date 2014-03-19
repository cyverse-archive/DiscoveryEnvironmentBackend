package org.iplantc.workflow.integration;

import org.iplantc.workflow.service.dto.FailedCategorizationList;
import org.iplantc.workflow.dao.TemplateGroupDao;
import java.util.HashSet;
import java.util.Set;
import org.iplantc.workflow.service.WorkspaceInitializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.service.dto.CategorizedAnalysis;
import org.iplantc.workflow.service.dto.AnalysisCategoryList;
import org.iplantc.workflow.service.dto.FailedCategorization;
import org.iplantc.workflow.service.dto.FavoriteUpdateRequest;
import org.iplantc.workflow.template.groups.TemplateGroup;

import static org.iplantc.workflow.util.ListUtils.conjoin;

/**
 * Used to categorize analyses.
 * 
 * @author Dennis Roberts
 */
public class AnalysisCategorizer {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * The index of the development analysis group.
     */
    private int devAnalysisGroupIndex;

    /**
     * The index of the favorites analysis group.
     */
    private int favoritesAnalysisGroupIndex;

    /**
     * Used to initialize the user's workspace.
     */
    private WorkspaceInitializer workspaceInitializer;

    /**
     * @param daoFactory used to obtain data access objects.
     * @param devAnalysisGroupIndex the index of the development analysis group.
     * @param favoritesAnalysisGroupIndex the index of the favorites analysis group.
     */
    public AnalysisCategorizer(DaoFactory daoFactory, int devAnalysisGroupIndex, int favoritesAnalysisGroupIndex,
            WorkspaceInitializer workspaceInitializer) {
        this.daoFactory = daoFactory;
        this.devAnalysisGroupIndex = devAnalysisGroupIndex;
        this.favoritesAnalysisGroupIndex = favoritesAnalysisGroupIndex;
        this.workspaceInitializer = workspaceInitializer;
    }

    /**
     * Updates a favorite.
     * 
     * @param request the favorite update request.
     */
    public void updateFavorite(FavoriteUpdateRequest request) {
        TransformationActivity analysis = getAnalysis(request.getAnalysisId());
        TemplateGroupImporter importer = createTemplateGroupImporter();
        if (request.isFavorite()) {
            importer.addAnalysisToFavorites(analysis, request.getWorkspaceId());
        }
        else {
            importer.removeAnalysisFromFavorites(analysis, request.getWorkspaceId());
        }
    }

    /**
     * Gets the public analysis categories for an analysis.
     * 
     * @param analysisSet a string used to specify which analyses to include ("all" or "public").
     * @return the analysis category for each public analysis.
     */
    public AnalysisCategoryList getAnalysisCategories(String analysisSet) {
        AnalysisCategoryList categories = new AnalysisCategoryList();
        List<Workspace> workspaces = getWorkspacesForAnalysisSet(analysisSet);
        for (Workspace workspace : workspaces) {
            addAnalysisCategoriesForWorkspace(categories, workspace);
        }
        return categories;
    }

    /**
     * Categorize a list of analyses.
     * 
     * @param categories the list of categorized analyses.
     */
    public FailedCategorizationList categorizeAnalyses(AnalysisCategoryList categories) {
        FailedCategorizationList failures = new FailedCategorizationList();
        Set<String> seen = new HashSet<String>();
        TemplateGroupPathResolver resolver = new TemplateGroupPathResolver(daoFactory, workspaceInitializer);
        for (CategorizedAnalysis category : categories) {
            try {
                TransformationActivity analysis = findAnalysis(category.getAnalysis().getId());
                decategorizeIfNotSeenAlready(seen, analysis);
                TemplateGroup templateGroup = resolver.resolvePath(category.getCategoryPath());
                templateGroup.addTemplate(analysis);
            }
            catch (WorkflowException e) {
                failures.addCategory(new FailedCategorization(category, e));
            }
        }
        return failures;
    }

    /**
     * Gets the list of workspaces for the given analysis set specifier.  If the specifier is "all" then all
     * workspaces are returned.  If the specifier is "public" then only public workspaces are returned.  Otherwise,
     * an empty list is returned.
     * 
     * @param analysisSet the analysis set specifier.
     * @return the workspaces for the analysis set specifier.
     */
    private List<Workspace> getWorkspacesForAnalysisSet(String analysisSet) {
        List<Workspace> workspaces;
        if (analysisSet.equalsIgnoreCase("public")) {
            workspaces = daoFactory.getWorkspaceDao().findPublicWorkspaces();
        }
        else if (analysisSet.equalsIgnoreCase("all")) {
            workspaces = daoFactory.getWorkspaceDao().findAll();
        }
        else {
            workspaces = new ArrayList<Workspace>();
        }
        return workspaces;
    }

    /**
     * Adds the analysis categories for a workspace.
     * 
     * @param categories the resulting list of analysis categories.
     * @param workspace the workspace.
     */
    private void addAnalysisCategoriesForWorkspace(AnalysisCategoryList categories, Workspace workspace) {
        Long rootAnalysisGroupId = workspace.getRootAnalysisGroupId();
        String username = workspace.getUser().getUsername();
        if (rootAnalysisGroupId != null) {
            TemplateGroup rootAnalysisGroup = daoFactory.getTemplateGroupDao().findByHid(rootAnalysisGroupId);
            if (rootAnalysisGroup != null) {
                List<String> analysisCategories = Arrays.asList(rootAnalysisGroup.getName());
                addAnalysisCategories(username, categories, rootAnalysisGroup, analysisCategories);
            }
        }
    }

    /**
     * Adds all of the analysis categories within a category tree to the analysis category list.
     * 
     * @param username the name of the user.
     * @param categories the resulting list of analysis categories.
     * @param category the current category.
     * @param names the names representing the path to the current category.
     */
    private void addAnalysisCategories(String username, AnalysisCategoryList categories, TemplateGroup category,
            List<String> names) {
        for (TransformationActivity analysis : category.getTemplates()) {
            if (!analysis.isDeleted()) {
                categories.addCategory(new CategorizedAnalysis(username, names, analysis));
            }
        }
        for (TemplateGroup subcategory : category.getSub_groups()) {
            addAnalysisCategories(username, categories, subcategory, conjoin(names, subcategory.getName()));
        }
    }

    /**
     * Finds the analysis to add to or remove from the user's favorites.
     * 
     * @param analysisId the analysis identifier.
     * @return the analysis.
     */
    private TransformationActivity getAnalysis(String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis, " + analysisId + " not found");
        }
        return analysis;
    }

    /**
     * Creates the template group importer to use.
     */
    private TemplateGroupImporter createTemplateGroupImporter() {
        return new TemplateGroupImporter(daoFactory, devAnalysisGroupIndex, favoritesAnalysisGroupIndex);
    }

    /**
     * Finds an analysis with the given identifier.
     * 
     * @param analysisId the analysis identifier.
     * @return the analysis.
     */
    private TransformationActivity findAnalysis(String analysisId) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis " + analysisId + " not found");
        }
        return analysis;
    }

    /**
     * Removes an analysis from all analysis categories if the analysis hasn't been seen already.
     * 
     * @param seen the set of analysis identifiers that have already been seen.
     * @param analysis the analysis.
     */
    private void decategorizeIfNotSeenAlready(Set<String> seen, TransformationActivity analysis) {
        if (!seen.contains(analysis.getId())) {
            seen.add(analysis.getId());
            TemplateGroupDao templateGroupDao = daoFactory.getTemplateGroupDao();
            List<TemplateGroup> groups = templateGroupDao.findTemplateGroupsContainingAnalysis(analysis);
            for (TemplateGroup group : groups) {
                group.removeTemplate(analysis);
                templateGroupDao.save(group);
            }
        }
    }
}
