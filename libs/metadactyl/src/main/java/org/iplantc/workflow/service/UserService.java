package org.iplantc.workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.authn.service.UserSessionService;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.persistence.dao.WorkspaceDao;
import org.iplantc.persistence.dao.user.UserDao;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.workflow.user.UserDetails;
import org.iplantc.workflow.user.UserInfo;

/**
 * A service that can be used to obtain information about a user.
 *
 * @author Dennis Roberts
 */
public class UserService {

    /**
     * The default name to use for the user's root analysis group.
     */
    private static final String DEFAULT_ROOT_ANALYSIS_GROUP_NAME = "Workspace";

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * The user session service.
     */
    private UserSessionService userSessionService;

    /**
     * The name of the user's root analysis group.
     */
    private String rootAnalysisGroup = DEFAULT_ROOT_ANALYSIS_GROUP_NAME;

    /**
     * The list of analysis group names to create underneath the user's root analysis group.
     */
    private List<String> defaultAnalysisGroups = new ArrayList<String>();

    /**
     * @param sessionFactory the Hibernate session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param userSessionService the user session service.
     */
    public void setUserSessionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    /**
     * @param rootAnalysisGroup the root analysis group name.
     */
    public void setRootAnalysisGroup(String rootAnalysisGroup) {
        this.rootAnalysisGroup = rootAnalysisGroup;
    }

    /**
     * @param defaultAnalysisGroups the new list of analysis group names as a JSON array string.
     */
    public void setDefaultAnalysisGroups(String defaultAnalysisGroups) {
        try {
            JSONArray names = (JSONArray) JSONSerializer.toJSON(defaultAnalysisGroups);
            this.defaultAnalysisGroups.clear();
            this.defaultAnalysisGroups.addAll(names);
        }
        catch (RuntimeException ignore) {
        }
    }

    /**
     * Gets information about the current user as a JSON string.
     *
     * @return the JSON string representing the current user.
     */
    public UserInfo getCurrentUserInfo() {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<UserInfo>() {
            @Override
            public UserInfo perform(Session session) {
                return getUserInfo(new HibernateDaoFactory(session));
            }
        });
    }

    /**
     * Gets information about the current user from the UserSessionService instance.
     *
     * @return the user details.
     * @throws org.iplantc.authn.exception.UserNotFoundException if the user can't be found.
     */
    public UserDetails getCurrentUserDetails() {
        org.iplantc.authn.user.User user = userSessionService.getUser();
        UserDetails userDetails = new UserDetails(user);
        return userDetails;
    }

    /**
     * Creates a user's workspace and any required components of the user's workspace if the user doesn't have a
     * workspace yet.  If the user does have a workspace and the required components of the workspace haven't been
     * created yet, then the components will be created.
     *
     * @param DaoFactory used to obtain data access objects.
     * @param username the user's e-mail address.
     * @return the workspace ID.
     */
    public Long createWorkspace(DaoFactory daoFactory, String username) {
        return getOrCreateWorkspaceForUsername(daoFactory, username).getId();
    }

    /**
     * Gets information about the current user.
     *
     * @param daoFactory used to obtain data access objects.
     * @return the user information.
     */
    private UserInfo getUserInfo(DaoFactory daoFactory) {
        return new UserInfo(getOrCreateWorkspace(daoFactory));
    }

    /**
     * Gets the user's workspace or creates a new workspace if the user doesn't have one yet.  If the user's workspace
     * doesn't have a root analysis category associated with it then the default analysis categories will be created.
     *
     * @note If a user doesn't exist for the currently logged in user a new user will be created as well.
     *
     * @param daoFactory used to obtain data access objects.
     * @return the workspace.
     */
    public Workspace getOrCreateWorkspace(DaoFactory daoFactory) {
        String username = userSessionService.getUser().getUsername();
        return getOrCreateWorkspaceForUsername(daoFactory, username);
    }

    /**
     * Gets the user's workspace or creates a new workspace if the user doesn't have one yet.  If the user's workspace
     * doesn't have a root analysis category associated with it then the default analysis categories will be created.
     *
     * @note If a user doesn't exist for the given username a new user will be created as well.
     *
     * @param daoFactory used to obtain data access objects.
     * @param username Username of the user we are creating a new workspace for.
     * @return the workspace.
     */
    public Workspace getOrCreateWorkspaceForUsername(DaoFactory daoFactory, String username) {
        return getOrCreateWorkspace(daoFactory, getOrCreateUser(daoFactory, username));
    }

    /**
     * Gets the user's workspace or creates a new workspace if the user doesn't have one yet.  If the user's workspace
     * doesn't have a root analysis category associated with it then the default analysis categories will be created.
     *
     * @param daoFactory used to obtain data access objects.
     * @param user the user we are creating a new workspace for.
     * @return the workspace.
     */
    private Workspace getOrCreateWorkspace(DaoFactory daoFactory, User user) {
        WorkspaceDao workspaceDao = daoFactory.getWorkspaceDao();

        Workspace workspace = workspaceDao.findByUser(user);
        if (workspace == null) {
            workspace = new Workspace();
            workspace.setIsPublic(false);
            workspace.setUser(user);
            workspace.setIsNew(true);
            workspaceDao.save(workspace);
        }

        if (workspace.getRootAnalysisGroupId() == null) {
            createAnalysisCategories(daoFactory, workspace);
        }

        return workspace;
    }

    /**
     * Gets or creates a new user.
     *
     * @param daoFactory used to obtain data access objects.
     * @param username the name of the user.
     * @return the user.
     */
    private User getOrCreateUser(DaoFactory daoFactory, String username) {
        UserDao userDao = daoFactory.getUserDao();
        User user = userDao.findByUsername(username);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            userDao.save(user);
        }
        return user;
    }

    /**
     * Creates the analysis categories.
     *
     * @param daoFactory used to obtain data access objects.
     * @param workspace the user's workspace.
     */
    private void createAnalysisCategories(DaoFactory daoFactory, Workspace workspace) {
        TemplateGroup rootAnalysisCategory = createTemplateGroup(workspace.getId(), rootAnalysisGroup);
        for (String subcategoryName : defaultAnalysisGroups) {
            rootAnalysisCategory.addGroup(createTemplateGroup(workspace.getId(), subcategoryName));
        }
        daoFactory.getTemplateGroupDao().save(rootAnalysisCategory);
        workspace.setRootAnalysisGroupId(rootAnalysisCategory.getHid());
        daoFactory.getWorkspaceDao().save(workspace);
    }

    /**
     * Creates a template group.
     *
     * @param workspaceId the workspace ID.
     * @param name the template group name.
     * @return the template group.
     */
    private TemplateGroup createTemplateGroup(long workspaceId, String name) {
        TemplateGroup templateGroup = new TemplateGroup();
        templateGroup.setId(UUID.randomUUID().toString());
        templateGroup.setName(name);
        templateGroup.setDescription("");
        templateGroup.setWorkspaceId(workspaceId);
        return templateGroup;
    }
}
