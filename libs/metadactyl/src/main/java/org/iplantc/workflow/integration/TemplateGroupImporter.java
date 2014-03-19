package org.iplantc.workflow.integration;

import java.util.List;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.template.groups.TemplateGroup;

/**
 * Used to import template groups into the database.
 * 
 * @author Dennis Roberts
 */
public class TemplateGroupImporter {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * The index of the analysis group in the user's workspace used for development.
     */
    private int devAnalysisGroupIndex;

    /**
     * The index of the "favorites" analysis group in the user's workspace.
     */
    private int favoritesAnalysisGroupIndex;

    /**
     * Initializes a new instance of this class.
     * 
     * @param daoFactory used to generate the template group data access object.
     */
    public TemplateGroupImporter(DaoFactory daoFactory, int devAnalysisGroupIndex, int favoritesAnalysisGroupIndex) {
        this.daoFactory = daoFactory;
        this.devAnalysisGroupIndex = checkedAnalysisGroupIndex(devAnalysisGroupIndex, "development");
        this.favoritesAnalysisGroupIndex = checkedAnalysisGroupIndex(favoritesAnalysisGroupIndex, "favorites");
    }

    /**
     * Verifies that an analysis group index is not negative.
     * 
     * @param index the index to validate.
     * @param description a description of the index.
     * @return the index.
     */
    private int checkedAnalysisGroupIndex(int index, String description) {
        if (index < 0) {
            throw new IllegalArgumentException("invalid " + description + " analysis group index: " + index);
        }
        return index;
    }

    /**
     * Adds an analysis to the default template group in the user's workspace for all newly imported analyses.
     * 
     * @param username the fully qualified username.
     * @param analysis the analysis to add to the default template group.
     * @throws WorkflowException if the user's development template group can't be found.
     */
    public void addAnalysisToWorkspace(String username, TransformationActivity analysis) throws WorkflowException {
        Workspace workspace = findWorkspace(username);
        TemplateGroup group = findDevTemplateGroup(workspace);
        group.addTemplate(analysis);
        daoFactory.getTemplateGroupDao().save(group);
    }

    /**
     * Adds an analysis to a user's favorites group.
     * 
     * @param analysis the analysis to add to the user's favorites group.
     * @param username the fully qualified username.
     */
    public void addAnalysisToFavorites(TransformationActivity analysis, String username) {
        TemplateGroup group = findFavoritesTemplateGroup(findWorkspace(username));
        validateFavoriteAddition(group, analysis);
        group.addTemplate(analysis);
        daoFactory.getTemplateGroupDao().save(group);
    }

    /**
     * Validates the addition of a new favorite analysis.
     * 
     * @param group the favorites analysis group.
     * @param analysis the analysis.
     * @throws WorkflowException if the analysis is already a favorite.
     */
    private void validateFavoriteAddition(TemplateGroup group, TransformationActivity analysis)
            throws WorkflowException {
        if (group.containsAnalysis(analysis)) {
            throw new WorkflowException("analysis, " + analysis.getId() + ", is already a favorite");
        }
    }

    /**
     * Adds an analysis to a user's favorites group.
     * 
     * @param analysis the analysis to add to the user's favorites group.
     * @param workspaceId the user's workspace ID.
     */
    public void addAnalysisToFavorites(TransformationActivity analysis, long workspaceId) {
        TemplateGroup group = findFavoritesTemplateGroup(findWorkspace(workspaceId));
        validateFavoriteAddition(group, analysis);
        group.addTemplate(analysis);
        daoFactory.getTemplateGroupDao().save(group);
    }

    /**
     * Removes an analysis from a user's favorites group.
     * 
     * @param analysis the analysis to remove from the user's template group.
     * @param username the fully qualified username.
     */
    public void removeAnalysisFromFavorites(TransformationActivity analysis, String username) {
        TemplateGroup group = findFavoritesTemplateGroup(findWorkspace(username));
        group.removeTemplate(analysis);
        daoFactory.getTemplateGroupDao().save(group);
    }

    /**
     * Removes an analysis from a user's favorites group.
     * 
     * @param analysis the analysis to remove from the user's template group.
     * @param workspaceId the user's workspace ID.
     */
    public void removeAnalysisFromFavorites(TransformationActivity analysis, long workspaceId) {
        TemplateGroup group = findFavoritesTemplateGroup(findWorkspace(workspaceId));
        group.removeTemplate(analysis);
        daoFactory.getTemplateGroupDao().save(group);
    }

    /**
     * Finds the favorites template group for the user.
     * 
     * @param workspace the user's workspace.
     * @return the template group.
     */
    private TemplateGroup findFavoritesTemplateGroup(Workspace workspace) {
        return findUserAnalysisGroup(workspace, favoritesAnalysisGroupIndex, "favorites");
    }

    /**
     * Finds the development template group for the user.
     * 0
     * @param workspace the user's workspace.
     * @return the template group.
     */
    private TemplateGroup findDevTemplateGroup(Workspace workspace) {
        return findUserAnalysisGroup(workspace, devAnalysisGroupIndex, "development");
    }

    /**
     * Finds the user analysis group at the given index.
     * 
     * @param workspace the workspace.
     * @param index the analysis group index.
     * @param description a brief description of the analysis group.
     * @return  the analysis group.
     */
    private TemplateGroup findUserAnalysisGroup(Workspace workspace, int index, String description) {
        TemplateGroup rootTemplateGroup = findRootTemplateGroup(workspace);
        List<TemplateGroup> subgroups = rootTemplateGroup.getSub_groups();
        if (index >= subgroups.size()) {
            throw new WorkflowException("unable to find the " + description + " analysis group");
        }
        return subgroups.get(index);
    }

    /**
     * Finds the workspace for a user.
     * 
     * @param username the fully qualified username.
     * @return the workspace.
     */
    private Workspace findWorkspace(String username) {
        User user = daoFactory.getUserDao().findByUsername(username);
        if (user == null) {
            throw new WorkflowException("user " + username + " not found");
        }
        Workspace workspace = daoFactory.getWorkspaceDao().findByUser(user);
        if (workspace == null || workspace.getRootAnalysisGroupId() == null) {
            throw new WorkflowException("workspace for " + username + " not initialized");
        }
        return workspace;
    }

    /**
     * Finds the workspace with the given identifier.
     * 
     * @param workspaceId the workspace identifier.
     * @return the workspace.
     */
    private Workspace findWorkspace(long workspaceId) {
        Workspace workspace = daoFactory.getWorkspaceDao().findById(workspaceId);
        if (workspace == null || workspace.getRootAnalysisGroupId() == null) {
            throw new WorkflowException("workspace id, " + workspaceId + ", not found");
        }
        return workspace;
    }

    /**
     * Finds the root template group for a workspace.
     * 
     * @param workspace the workspace.
     * @return the root template group.
     */
    private TemplateGroup findRootTemplateGroup(Workspace workspace) {
        TemplateGroup root = daoFactory.getTemplateGroupDao().findByHid(workspace.getRootAnalysisGroupId());
        if (root == null) {
            throw new WorkflowException("root template group not found for workspace " + workspace.getId());
        }
        return root;
    }
}
