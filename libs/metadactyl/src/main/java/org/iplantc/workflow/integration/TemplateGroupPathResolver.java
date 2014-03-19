package org.iplantc.workflow.integration;

import java.util.List;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.integration.util.ImportUtils;
import org.iplantc.workflow.service.WorkspaceInitializer;
import org.iplantc.workflow.service.dto.CategoryPath;
import org.iplantc.workflow.template.groups.TemplateGroup;

/**
 * Resolves paths to template groups.
 * 
 * @author Dennis Roberts
 */
public class TemplateGroupPathResolver {

    /**
     * Used to obtain data access objects.
     */
    private DaoFactory daoFactory;

    /**
     * Used to retrieve workspaces.
     */
    private WorkspaceInitializer workspaceInitializer;

    /**
     * @param daoFactory the data access object factory.
     * @param workspaceInitializer used to initialize workspaces.
     */
    public TemplateGroupPathResolver(DaoFactory daoFactory, WorkspaceInitializer workspaceInitializer) {
        this.daoFactory = daoFactory;
        this.workspaceInitializer = workspaceInitializer;
    }

    /**
     * @param path the full path to the category.
     * @return the template group.
     */
    public TemplateGroup resolvePath(CategoryPath path) {
        return resolvePath(path.getUsername(), path.getPath());
    }

    /**
     * Resolves a path to a template group.
     * 
     * @param username the name of the user.
     * @param path the path to the template group.
     * @return the template group.
     */
    public TemplateGroup resolvePath(String username, List<String> path) {
        Workspace workspace = getWorkspace(username);
        TemplateGroup root = getRootTemplateGroupForWorkspace(workspace, path.get(0));
        return resolvePath(root, path.subList(1, path.size()));
    }

    /**
     * Gets the root template group for a workspace.
     * 
     * @param workspace the workspace.
     * @param rootName the name of the root template group.
     * @return the root template group for the workspace.
     */
    private TemplateGroup getRootTemplateGroupForWorkspace(Workspace workspace, String rootName) {
        TemplateGroup root = null;
        Long rootId = workspace.getRootAnalysisGroupId();
        if (rootId == null) {
            root = createTemplateGroup(rootName);
            daoFactory.getTemplateGroupDao().save(root);
            workspace.setRootAnalysisGroupId(root.getHid());
            daoFactory.getWorkspaceDao().save(workspace);
        }
        else {
            root = daoFactory.getTemplateGroupDao().findByHid(rootId);
            validateRootTemplateGroup(root, workspace.getUser().getUsername(), rootName);
        }
        return root;
    }

    /**
     * Validates a root template group that was retrieved from a workspace.
     * 
     * @param root the root template group to validate.
     * @param username the name of the user.
     * @param rootName the expected name of the root template group.
     */
    private void validateRootTemplateGroup(TemplateGroup root, String username, String rootName) {
        if (root == null) {
            String msg = "root template group referenced by workspace for " + username + " does not exist";
            throw new WorkflowException(msg);
        }
        if (!root.getName().equals(rootName)) {
            String msg = "root template group referenced by workspace for " + username + " does not match "
                    + "specified root template group name";
            throw new WorkflowException(msg);
        }
    }

    /**
     * Resolves a path relative to a template group.
     * 
     * @param curr the current template group.
     * @param path the path relative to the current template group.
     * @return the resolved template group.
     */
    private TemplateGroup resolvePath(TemplateGroup curr, List<String> path) {
        for (String name : path) {
            TemplateGroup next = curr.getSubgroup(name);
            if (next == null) {
                next = createTemplateGroup(name);
                curr.addGroup(next);
                daoFactory.getTemplateGroupDao().save(curr);
            }
            curr = next;
        }
        return curr;
    }

    /**
     * Retrieves a workspace, throwing an exception if the workspace can't be found.
     * 
     * @param username the name of the user.
     * @return the workspace.
     */
    private Workspace getWorkspace(String username) {
        return workspaceInitializer.getWorkspace(daoFactory, username);
    }

    /**
     * Creates a new template group.
     * 
     * @param name the name of the template group.
     * @return the template group.
     */
    private TemplateGroup createTemplateGroup(String name) {
        TemplateGroup templateGroup = new TemplateGroup();
        templateGroup.setId(ImportUtils.generateId());
        templateGroup.setName(name);
        return templateGroup;
    }
}
