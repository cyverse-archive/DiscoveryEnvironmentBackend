package org.iplantc.workflow.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.mock.MockWorkspaceInitializer;
import org.iplantc.workflow.service.UserService;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.TemplateGroupImporter.
 * 
 * @author Dennis Roberts
 */
public class TemplateGroupImporterTest {

    /**
     * The name of the user's root analysis group.
     */
    private static final String ROOT_ANALYSIS_GROUP = "Workspace";

    /**
     * The name of the development analysis group.
     */
    private static final String DEV_ANALYSIS_GROUP = "Dev";

    /**
     * The name of the favorites analysis group.
     */
    private static final String FAVES_ANALYSIS_GROUP = "Faves";

    /**
     * The list of analysis subgroup names.
     */
    private static final List<String> ANALYSIS_SUBGROUPS = Arrays.asList(DEV_ANALYSIS_GROUP, FAVES_ANALYSIS_GROUP);

    /**
     * The fully qualified username to use during testing.
     */
    private static final String USERNAME = "somebody@somewhere.net";

    /**
     * Used to generate mock data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The template group importer that is being tested.
     */
    private TemplateGroupImporter importer;

    /**
     * The user service used to initialize the user's workspace.
     */
    private UserService userService;

    /**
     * The initializer that calls the user service.
     */
    private MockWorkspaceInitializer workspaceInitializer;

    /**
     * Sets up each of the tests.
     */
    @Before
    public void setUp() {
        daoFactory = new MockDaoFactory();
        importer = new TemplateGroupImporter(daoFactory, 0, 1);
        initializeWorkspaceInitializer();
        initializeUserService();
        userService.createWorkspace(daoFactory, USERNAME);
    }

    /**
     * Gets the user workspace.
     * 
     * @return the workspace.
     */
    private Workspace getUserWorkspace() {
        User user = daoFactory.getUserDao().findByUsername(USERNAME);
        return daoFactory.getWorkspaceDao().findByUser(user);
    }

    /**
     * Initializes the object used to initialize the the user's workspace.
     */
    private void initializeWorkspaceInitializer() {
        workspaceInitializer = new MockWorkspaceInitializer(userService);
    }

    /**
     * Initializes the service that actually initializes the user's workspace.
     */
    private void initializeUserService() {
        userService = new UserService();
        userService.setRootAnalysisGroup(ROOT_ANALYSIS_GROUP);
        userService.setDefaultAnalysisGroups(new JSONArray(ANALYSIS_SUBGROUPS).toString());
    }

    /**
     * Verifies that we can add an analysis to the user's development analysis group.
     */
    @Test
    public void shouldAddAnalysisToDefaultTemplateGroup() {
        TransformationActivity analysis = createAnalysis();
        importer.addAnalysisToWorkspace(USERNAME, analysis);
        assertTrue(analysesInGroup(analysis, DEV_ANALYSIS_GROUP));
    }

    /**
     * Verifies that we can add an analysis to the user's favorites analysis group.
     */
    @Test
    public void shouldAddAnalysisToFavoritesTemplateGroupUsingUsername() {
        TransformationActivity analysis = createAnalysis();
        importer.addAnalysisToFavorites(analysis, USERNAME);
        assertTrue(analysesInGroup(analysis, FAVES_ANALYSIS_GROUP));
    }

    /**
     * Verifies that we can add an analysis to the user's favorites analysis group.
     */
    @Test
    public void shouldAddAnalysisToFavoritesTemplateGroupUsingWorkspaceId() {
        TransformationActivity analysis = createAnalysis();
        importer.addAnalysisToFavorites(analysis, getUserWorkspace().getId());
        assertTrue(analysesInGroup(analysis, FAVES_ANALYSIS_GROUP));
    }

    /**
     * Verifies that we can remove an analysis from the user's favorites analysis group.
     */
    @Test
    public void shouldRemoveAnalysisFromFavoritesTemplateGroupUsingUsername() {
        TransformationActivity analysis = createAnalysis();
        importer.addAnalysisToFavorites(analysis, USERNAME);
        assertTrue(analysesInGroup(analysis, FAVES_ANALYSIS_GROUP));
        importer.removeAnalysisFromFavorites(analysis, USERNAME);
        assertFalse(analysesInGroup(analysis, FAVES_ANALYSIS_GROUP));
    }

    /**
     * Verifies that we can remove an analysis from the user's favorites analysis group.
     */
    @Test
    public void shouldRemoveAnalysisFromFavoritesTemplateGroupUsingWorkspaceId() {
        long workspaceId = getUserWorkspace().getId();
        TransformationActivity analysis = createAnalysis();
        importer.addAnalysisToFavorites(analysis, workspaceId);
        assertTrue(analysesInGroup(analysis, FAVES_ANALYSIS_GROUP));
        importer.removeAnalysisFromFavorites(analysis, workspaceId);
        assertFalse(analysesInGroup(analysis, FAVES_ANALYSIS_GROUP));
    }

    /**
     * Verifies that we get an exception for an unknown user.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUnknownUser() {
        importer.addAnalysisToFavorites(createAnalysis(), "nobody@nowhere.net");
    }

    /**
     * Verifies that we get an exception for a user with no workspace.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUserWithoutWorkspace() {
        String username = "nobody@nowhere.net";
        addUser(username);
        importer.addAnalysisToFavorites(createAnalysis(), username);
    }

    /**
     * Verifies that we get an exception for a user with an uninitialized workspace.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUninitializedWorkspace() {
        String username = "nobody@nowhere.net";
        addUninitializedWorkspace(username);
        importer.addAnalysisToFavorites(createAnalysis(), username);
    }

    /**
     * Verifies that we get an exception for a user without a favorites category.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForWorkspaceWithoutSubcategories() {
        String username = "nobody@nowhere.net";
        addWorkspaceWithoutSubcategories(username);
        importer.addAnalysisToFavorites(createAnalysis(), username);
    }

    /**
     * Verifies that we get an exception if we try to mark the same analysis as a favorite twice.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForDuplicateFavorite() {
        TransformationActivity analysis = createAnalysis();
        importer.addAnalysisToFavorites(analysis, USERNAME);
        importer.addAnalysisToFavorites(analysis, USERNAME);
    }

    /**
     * Creates an analysis that is partially initialized for the user with the given username.
     * 
     * @param username the fully qualified username.
     * @return the workspace.
     */
    private Workspace addWorkspaceWithoutSubcategories(String username) {
        Workspace workspace = addUninitializedWorkspace(username);
        TemplateGroup root = new TemplateGroup();
        root.setName("workspace");
        root.setId("someid");
        root.setDescription("root template group for " + username);
        root.setWorkspaceId(workspace.getId());
        daoFactory.getTemplateGroupDao().save(root);
        workspace.setRootAnalysisGroupId(root.getHid());
        return workspace;
    }

    /**
     * Adds an uninitialized workspace for the user with the given username.
     * 
     * @param username the fully qualified username.
     * @return the workspace.
     */
    private Workspace addUninitializedWorkspace(String username) {
        User user = addUser(username);
        Workspace workspace = new Workspace();
        workspace.setUser(user);
        daoFactory.getWorkspaceDao().save(workspace);
        return workspace;
    }

    /**
     * Adds a user with the given username.
     * 
     * @param username the fully qualified username.
     * @return the user.
     */
    private User addUser(String username) {
        User user = new User();
        user.setUsername("nobody@nowhere.net");
        daoFactory.getUserDao().save(user);
        return user;
    }

    /**
     * Creates an analysis for testing.
     * 
     * @return the analysis.
     */
    private TransformationActivity createAnalysis() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setName("an ordinary analysis on an ordinary night");
        analysis.setIntegrationDatum(createIntegrationDatum());
        return analysis;
    }

    /**
     * Creates an integration datum for testing.
     *
     * @return the integration datum.
     */
    private IntegrationDatum createIntegrationDatum() {
        IntegrationDatum integrationDatum = new IntegrationDatum();
        integrationDatum.setIntegratorEmail(USERNAME);
        return integrationDatum;
    }

    /**
     * Determines whether or not an analysis is in an analysis group.
     * 
     * @param analysis the analysis.
     * @param groupName the template group name.
     * @return true if the analysis is in the group.
     */
    private boolean analysesInGroup(TransformationActivity analysis, String groupName) {
        TemplateGroup group = findRootTemplateGroup().getSub_groups().get(ANALYSIS_SUBGROUPS.indexOf(groupName));
        return group.getTemplates().contains(analysis);
    }

    /**
     * Finds the user's root template group.
     */
    private TemplateGroup findRootTemplateGroup() {
        return daoFactory.getTemplateGroupDao().findByHid(getUserWorkspace().getRootAnalysisGroupId());
    }
}
