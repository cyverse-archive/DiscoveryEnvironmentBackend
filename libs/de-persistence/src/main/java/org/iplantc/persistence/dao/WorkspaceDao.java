package org.iplantc.persistence.dao;

import java.util.List;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;

/**
 * @author Dennis Roberts
 */
public interface WorkspaceDao extends GenericDao<Workspace> {

    /**
     * Deletes a user's workspace.
     * 
     * @param username the user ID.
     */
    public void deleteByUser(User user);

    /**
     * Finds a user's workspace.
     * 
     * @param user The user we are retrieving the workspace for.
     * @return the workspace or null if a matching workspace can't be found.
     */
    public Workspace findByUser(User user);

    /**
     * Finds public workspaces.
     * 
     * @return the list of public workspaces.
     */
    public List<Workspace> findPublicWorkspaces();
}
