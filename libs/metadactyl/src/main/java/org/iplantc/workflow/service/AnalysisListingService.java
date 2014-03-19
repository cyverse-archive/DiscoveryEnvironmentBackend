package org.iplantc.workflow.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.iplantc.hibernate.util.SessionTask;
import org.iplantc.hibernate.util.SessionTaskWrapper;
import org.iplantc.persistence.dto.listing.AnalysisGroup;
import org.iplantc.persistence.dto.listing.AnalysisListing;
import org.iplantc.persistence.dto.listing.RatingListing;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.service.dto.analysis.DeployedComponentListDto;
import org.iplantc.workflow.service.dto.analysis.list.AnalysisGroupDto;
import org.iplantc.workflow.service.dto.analysis.list.AnalysisGroupHierarchyList;
import org.iplantc.workflow.service.dto.analysis.list.AnalysisGroupList;
import org.iplantc.workflow.service.dto.analysis.list.AnalysisList;
import org.iplantc.workflow.service.dto.analysis.list.UserRating;

/**
 * A service used to list analyses.
 *
 * @author Dennis Roberts
 */
public class AnalysisListingService {

    /**
     * The Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * The list index of the favorites analysis group within the user's root analysis group.
     */
    private int favoritesAnalysisGroupIndex;

    /**
     * Used to initialize the user's workspace.
     */
    private WorkspaceInitializer workspaceInitializer;

    /**
     * @param sessionFactory the Hibernate session factory.
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param favoritesAnalysisGroupIndex the list index of the favorites analysis group.
     */
    public void setFavoritesAnalysisGroupIndex(int favoritesAnalysisGroupIndex) {
        this.favoritesAnalysisGroupIndex = favoritesAnalysisGroupIndex;
    }

    /**
     * @param workspaceInitializer used to initialize the user's workspace.
     */
    public void setWorkspaceInitializer(WorkspaceInitializer workspaceInitializer) {
        this.workspaceInitializer = workspaceInitializer;
    }

    /**
     * Lists the analysis group hierarchy.
     *
     * @param workspaceToken either the workspace identifier or the user's e-mail address.
     * @return a JSON string representing the analysis group hierarchy listings.
     */
    public String listAnalysisGroups(final String workspaceToken) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                DaoFactory daoFactory = new HibernateDaoFactory(session);
                AnalysisGroupFinder analysisGroupFinder = new AnalysisGroupFinder(daoFactory);
                List<AnalysisGroup> groups = analysisGroupFinder.findDefaultGroups(workspaceToken);
                return new AnalysisGroupHierarchyList(groups, daoFactory).toString();
            }
        });
    }

    /**
     * Lists all of the public analyses.
     *
     * @return a JSON string representing the list of public analyses.
     */
    public String listPublicAnalyses() {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                DaoFactory daoFactory = new HibernateDaoFactory(session);
                AnalysisGroupFinder analysisGroupFinder = new AnalysisGroupFinder(daoFactory);
                List<AnalysisGroup> groups = analysisGroupFinder.findPublicGroups();
                return new AnalysisGroupList(groups).toString();
            }
        });
    }

    /**
     * Lists all analyses that are visible to a user.
     *
     * @param analysisGroupId the group ID
     * @return a JSON string representing the list of public analyses.
     */
    public String listAnalysesInGroup(final String analysisGroupId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                DaoFactory daoFactory = new HibernateDaoFactory(session);
                AnalysisGroupFinder analysisGroupFinder = new AnalysisGroupFinder(daoFactory);
                Workspace workspace = workspaceInitializer.getWorkspace(daoFactory);
                AnalysisGroup favoritesGroup = analysisGroupFinder.findFavoritesGroup();
                Set<AnalysisListing> favorites = new HashSet<AnalysisListing>(favoritesGroup.getAllActiveAnalyses());
                AnalysisGroup group = analysisGroupFinder.findGroup(analysisGroupId);
                Map<Long, UserRating> userRatings = loadUserRatings(workspace.getUser(),
                        daoFactory);
                return new AnalysisGroupDto(group, favorites, userRatings).toString();
            }

            private Map<Long, UserRating> loadUserRatings(User user, DaoFactory daoFactory) {
                Map<Long, UserRating> result = new HashMap<Long, UserRating>();
                for (RatingListing rating : daoFactory.getRatingListingDao().findByUser(user)) {
                    UserRating ratingPojo = new UserRating(rating.getUserRating(),
                            rating.getCommentId());
                    result.put(new Long(rating.getAnalysisId()), ratingPojo);
                }
                return result;
            }
        });
    }

    /**
     * Lists an analysis corresponding to a given identifier.  The result is a JSON string representing an object
     * containing a list of analyses.  If an analysis with the given identifier exists then the list will contain
     * that analysis.  Otherwise, the list will be empty.
     *
     * @param analysisId the analysis identifier.
     * @return a JSON string representing an object containing a list of analyses.
     */
    public String listAnalysis(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                return new AnalysisList(new HibernateDaoFactory(session), analysisId).toString();
            }
        });
    }

    /**
     * Lists all of the deployed components associated with an analysis.  There may be multiple deployed components
     * associated with a single analysis if the analysis happens to have multiple steps.
     *
     * @param analysisId the analysis identifier.
     * @return the list of deployed components.
     */
    public String listDeployedComponentsInAnalysis(final String analysisId) {
        return new SessionTaskWrapper(sessionFactory).performTask(new SessionTask<String>() {
            @Override
            public String perform(Session session) {
                DaoFactory daoFactory = new HibernateDaoFactory(session);
                return new DeployedComponentListDto(new AnalysisListingLoader(daoFactory).load(analysisId)).toString();
            }
        });
    }

    /**
     * Finds analysis groups.
     */
    private class AnalysisGroupFinder {

        /**
         * Used to obtain data access objects.
         */
        private DaoFactory daoFactory;

        /**
         * @param daoFactory used to obtain data access objects
         */
        public AnalysisGroupFinder(DaoFactory daoFactory) {
            this.daoFactory = daoFactory;
        }

        /**
         * Finds and returns the list of root public analysis groups.
         *
         * @return the list of root public analysis groups.
         */
        public List<AnalysisGroup> findPublicGroups() {
            List<AnalysisGroup> groups = new ArrayList<AnalysisGroup>();
            addPublicGroups(groups);
            return groups;
        }

        /**
         * Finds and returns the list of root analysis groups that are visible
         * to the user with the specified workspace token.
         *
         * @param workspaceToken either the workspace id or the user's e-mail address
         * @return the list of root analysis groups that are visible to the user
         */
        public List<AnalysisGroup> findDefaultGroups(String workspaceToken) {
            return findDefaultGroups(getWorkspace(workspaceToken));
        }

        /**
         * Finds and returns the list of root analysis groups that are visible
         * to the user with the specified workspace.
         *
         * @param workspace the user's workspace
         * @return the list of root analysis groups that are visible to the user
         */
        public List<AnalysisGroup> findDefaultGroups(Workspace workspace) {
            List<AnalysisGroup> groups = new ArrayList<AnalysisGroup>();

            addRootGroupForUser(groups, workspace);
            addPublicGroups(groups);

            return groups;
        }

        /**
         * Finds the analysis group containing the current user's favorite analyses.
         *
         * @return the analysis group
         * @throws WorkflowException if the user doesn't have a favorites group
         */
        public AnalysisGroup findFavoritesGroup() {
            Workspace workspace = workspaceInitializer.getWorkspace(daoFactory);
            AnalysisGroup root = daoFactory.getAnalysisGroupDao().findById(workspace.getRootAnalysisGroupId());
            if (root == null) {
                throw new WorkflowException("user's workspace is not initializeed");
            }
            if (root.getSubgroups().size() <= favoritesAnalysisGroupIndex) {
                throw new WorkflowException("unable to find favorites group in user's workspace");
            }
            return root.getSubgroups().get(favoritesAnalysisGroupIndex);
        }

        /**
         * Finds the analysis group with the given identifier.
         *
         * @param groupId the analysis group identifier
         * @return the analysis group
         * @throws WorkflowException if the analysis group isn't found
         */
        public AnalysisGroup findGroup(String groupId) {
            AnalysisGroup group = daoFactory.getAnalysisGroupDao().findByExternalId(groupId);
            if (group == null) {
                throw new WorkflowException("analysis group " + groupId + " not found");
            }
            return group;
        }

        /**
         * Adds the public root analysis groups to a list of analysis groups.
         *
         * @param groups the list of analysis groups
         */
        private void addPublicGroups(List<AnalysisGroup> groups) {
            for (Workspace workspace : daoFactory.getWorkspaceDao().findPublicWorkspaces()) {
                AnalysisGroup group = getRootAnalysisGroupForWorkspace(workspace);
                if (group != null && !groups.contains(group)) {
                    groups.add(group);
                }
            }
        }

        /**
         * Gets the root analysis group for a workspace.
         *
         * @param workspace the workspace
         * @return the root analysis group
         */
        private AnalysisGroup getRootAnalysisGroupForWorkspace(Workspace workspace) {
            Long groupId = workspace.getRootAnalysisGroupId();
            return groupId == null ? null : daoFactory.getAnalysisGroupDao().findById(groupId);
        }

        /**
         * Adds the root analysis group for a user to a list of analysis groups.
         *
         * @param groups the list of analysis groups
         * @param workspace the user's workspace
         */
        private void addRootGroupForUser(List<AnalysisGroup> groups, Workspace workspace) {
            if (workspace != null) {
                AnalysisGroup templateGroup = getRootAnalysisGroupForWorkspace(workspace);
                if (templateGroup != null) {
                    groups.add(templateGroup);
                }
            }
        }

        /**
         * Gets the workspace for a workspace identifier or e-mail address.
         *
         * @param workspaceToken the workspace identifier or e-mail address
         * @return the workspace or null if a workspace can't be found
         */
        private Workspace getWorkspace(String workspaceToken) {
            Workspace workspace;
            if (StringUtils.isNumeric(workspaceToken)) {
                workspace = daoFactory.getWorkspaceDao().findById(Long.parseLong(workspaceToken));
            }
            else {
                User user = daoFactory.getUserDao().findByUsername(workspaceToken);
                workspace = daoFactory.getWorkspaceDao().findByUser(user);
            }
            return workspace;
        }
    }
}
