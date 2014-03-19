package org.iplantc.workflow.service;

import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.dao.DaoFactory;

/**
 * An injectable {@link WorkspaceInitializer} implementation that calls the {@link UserService} directly.
 * 
 * @author Dennis Roberts
 */
public class InjectableWorkspaceInitializer implements WorkspaceInitializer {

    /**
     * The service used to initialize the user's workspace and obtain user info.
     */
    private UserService userService;

    /**
     * @param userService the service used to initialize the user's workspace and obtain user info.
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeWorkspace(DaoFactory daoFactory, String username) {
        userService.createWorkspace(daoFactory, username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workspace getWorkspace(DaoFactory daoFactory) {
        return userService.getOrCreateWorkspace(daoFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workspace getWorkspace(DaoFactory daoFactory, String username) {
        return userService.getOrCreateWorkspaceForUsername(daoFactory, username);
    }
}
