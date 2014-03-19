package org.iplantc.workflow.user;

import org.iplantc.persistence.dto.workspace.Workspace;

/**
 * Encapsulated user information.
 *
 * @author Dennis Roberts
 */
public class UserInfo {

    /**
     * The workspace ID.
     */
    private final String workspaceId;

    /**
     * True if the workspace is new.
     */
    private final boolean newWorkspace;

    /**
     * @param workspace the user's workspace.
     */
    public UserInfo(Workspace workspace) {
        workspaceId = String.valueOf(workspace.getId());
        newWorkspace = workspace.getIsNew();
    }

    /**
     * @return the workspace ID.
     */
    public String getWorkspaceId() {
        return workspaceId;
    }

    /**
     * @return true if the workspace was just created.
     */
    public boolean isNewWorkspace() {
        return newWorkspace;
    }
}
