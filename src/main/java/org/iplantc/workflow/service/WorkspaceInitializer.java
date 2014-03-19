package org.iplantc.workflow.service;

import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * Used to initialize a user's workspace.
 *
 * @author Dennis Roberts
 */
public interface WorkspaceInitializer {

    /**
     * Initializes a user's workspace.
     *
     * @param daoFactory used to obtain data access objects.
     * @param username the user name.
     */
    public void initializeWorkspace(DaoFactory daoFactory, String username);

    /**
     * Gets the workspace of the current user.
     * 
     * @param daoFactory used to obtain data access objects.
     * @return the workspace.
     */
    public Workspace getWorkspace(DaoFactory daoFactory);

    /**
     * Gets the workspace of the specified user.
     * 
     * @param daoFactory used to obtain data access objects.
     * @param username the name of the user.
     * @return the workspace.
     */
    public Workspace getWorkspace(DaoFactory daoFactory, String username);
}
