package org.iplantc.workflow.mock;

import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.service.UserService;
import org.iplantc.workflow.service.WorkspaceInitializer;

/**
 * A mock workspace initializer used for unit testing.
 *
 * @author Dennis Roberts
 */
public class MockWorkspaceInitializer implements WorkspaceInitializer {

    /**
     * Used to initialize the user's workspace.
     */
    private UserService userService;

    /**
     * @param userService the service used to initialize the user's workspace.
     */
    public MockWorkspaceInitializer(UserService userService) {
        this.userService = userService;
    }

    /**
     * Initializes the user's workspace.
     * 
     * @param daoFactory used to obtain data access objects.
     * @param username the name of the user.
     */
    @Override
    public void initializeWorkspace(DaoFactory daoFactory, String username) {
        userService.createWorkspace(daoFactory, username);
    }

    /**
     * Gets the current user's workspace.
     * 
     * @param daoFactory used to obtain data access objects.
     * @return the workspace.
     */
    @Override
    public Workspace getWorkspace(DaoFactory daoFactory) {
        return userService.getOrCreateWorkspace(daoFactory);
    }

    /**
     * Gets the current user's workspace.
     * 
     * @param daoFactory used to obtain data access objects.
     * @param username the name of the user.
     * @return the workspace.
     */
    @Override
    public Workspace getWorkspace(DaoFactory daoFactory, String username) {
        return userService.getOrCreateWorkspaceForUsername(daoFactory, username);
    }
}
