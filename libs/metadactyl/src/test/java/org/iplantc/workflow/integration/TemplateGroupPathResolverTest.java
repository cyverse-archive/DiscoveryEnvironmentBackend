package org.iplantc.workflow.integration;

import org.iplantc.workflow.service.dto.CategoryPath;
import org.iplantc.workflow.service.UserService;
import java.util.List;
import java.util.Arrays;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.mock.MockWorkspaceInitializer;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.integration.TemplateGroupPathResolver.
 * 
 * @author Dennis Roberts
 */
public class TemplateGroupPathResolverTest {

    /**
     * A mock DAO factory for testing.
     */
    private MockDaoFactory daoFactory;

    /**
     * A user service to use for testing.
     */
    private UserService userService;

    /**
     * A mock workspace initializer for testing.
     */
    private MockWorkspaceInitializer workspaceInitialzer;

    /**
     * An instance of the template group path resolver for testing.
     */
    private TemplateGroupPathResolver resolver;

    @Before
    public void setUp() {
        daoFactory = new MockDaoFactory();
        initializeUserService();
        addTestWorkspaces();
        addTestTemplateGroups();
        resolver = new TemplateGroupPathResolver(daoFactory, new MockWorkspaceInitializer(userService));
    }

    /**
     * Verifies that we get an exception for a non-existent workspace ID.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForNonExistentWorkspace() {
        resolver.resolvePath(new CategoryPath("I don't exist!", Arrays.asList("foo", "bar", "baz")));
    }

    /**
     * Verifies that the resolver can resolve a simple path.
     */
    @Test
    public void shouldResolveSimplePath() {
        TemplateGroup actual = resolver.resolvePath("public", Arrays.asList("Public Applications"));
        TemplateGroup expected = daoFactory.getTemplateGroupDao().findUniqueInstanceByName("Public Applications");
        assertSame(expected, actual);
    }

    /**
     * Verifies that the resolver can resolve a compound path.
     */
    @Test
    public void shouldResolveCompoundPath() {
        List<String> path = Arrays.asList("Public Applications", "foo", "bar", "baz");
        TemplateGroup actual = resolver.resolvePath("public", path);
        TemplateGroup expected = daoFactory.getTemplateGroupDao().findUniqueInstanceByName("baz");
        assertSame(expected, actual);
    }

    /**
     * Verifies that the resolver can resolve a user's private simple path.
     */
    @Test
    public void shouldResolvePrivateSimplePath() {
        List<String> path = Arrays.asList("Workspace");
        TemplateGroup actual = resolver.resolvePath("somebody", path);
        TemplateGroup expected = testResolve("somebody");
        assertSame(expected, actual);
    }

    /**
     * Verifies that the resolver can resolve a user's private compound path.
     */
    @Test
    public void shouldResolvePrivateCompoundPath() {
        List<String> path = Arrays.asList("Workspace", "coo", "car", "caz");
        TemplateGroup actual = resolver.resolvePath("somebody", path);
        TemplateGroup expected = testResolve("somebody", "coo", "car", "caz");
        assertSame(expected, actual);
    }

    /**
     * Verifies that the resolver validates the root template group name.
     */
    @Test
    public void shouldValidateRootTemplateGroupName() {
        List<String> path = Arrays.asList("Workspace");
        assertNotNull(resolver.resolvePath("nobody", path));
        assertNotNull(getWorkspaceForUser("nobody").getRootAnalysisGroupId());
    }

    /**
     * Verifies that the resolver creates template groups that don't exist yet.
     */
    @Test
    public void shouldCreateUndefinedSubgroups() {
        List<String> path = Arrays.asList("Workspace", "foo", "bar", "baz");
        TemplateGroup actual = resolver.resolvePath("nobody", path);
        assertSame(testResolve("nobody", "foo", "bar", "baz"), actual);
        assertNotSame(testResolve("public", "foo", "bar", "baz"), actual);
    }

    /**
     * Verifies that we get an exception if the workspace refers to an undefined root template group.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForNonExistentRootTemplateGroupId() {
        List<String> path = Arrays.asList("Workspace");
        resolver.resolvePath("jrhacker", path);
    }

    /**
     * Verifies that we get an exception of the specified root template group name doesn't match the actual root
     * template group name.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForMismatchedRootTemplateGroupName() {
        List<String> path = Arrays.asList("Unknown Space");
        resolver.resolvePath("somebody", path);
    }

    /**
     * Initializes the user service.
     */
    private void initializeUserService() {
        userService = new UserService();
        userService.setRootAnalysisGroup("Workspace");
    }

    /**
     * Adds some workspaces to the DAO for testing.
     */
    private void addTestWorkspaces() {
        addWorkspace("public", null);
        addWorkspace("somebody", null);
        addWorkspace("nobody", null);
        addWorkspace("jrhacker", 99L);
    }

    /**
     * Adds a user and a workspace to their respective DAOs.
     * 
     * @param username the username.
     * @param rootAnalysisGroupId the root analysis group identifier.
     */
    private void addWorkspace(String username, Long rootAnalysisGroupId) {
        Workspace workspace = UnitTestUtils.createWorkspace(username, rootAnalysisGroupId);
        daoFactory.getUserDao().save(workspace.getUser());
        daoFactory.getWorkspaceDao().save(workspace);
    }

    /**
     * Adds some template groups to the DAO for testing.
     */
    private void addTestTemplateGroups() {
        saveTemplateGroupHierarchy("public", "Public Applications", "foo", "bar", "baz");
        saveTemplateGroupHierarchy("public", "Public Applications", "foo", "bar", "quux");
        saveTemplateGroupHierarchy("somebody", "Workspace", "coo", "car", "caz");
        saveTemplateGroupHierarchy("somebody", "Workspace", "coo", "quax", "glarb");
    }

    /**
     * Finds the root template group for a workspace.
     * 
     * @param workspace the workspace.
     * @return the root template group or null if the workspace doesn't have a root template group.
     */
    private TemplateGroup getRootTemplateGroupForWorkspace(Workspace workspace) {
        TemplateGroup root = null;
        if (workspace.getRootAnalysisGroupId() != null) {
            return daoFactory.getTemplateGroupDao().findByHid(workspace.getRootAnalysisGroupId());
        }
        return root;
    }

    /**
     * Adds a template group hierarchy to the DAO for testing.
     * 
     * @param username the name of the user.
     * @param rootName the root template group name.
     * @param names 
     */
    private void saveTemplateGroupHierarchy(String username, String rootName, String... names) {
        TemplateGroup current = getRootTemplateGroup(username, rootName);
        for (String name : names) {
            TemplateGroup next = current.getSubgroup(name);
            if (next == null) {
                next = UnitTestUtils.createTemplateGroup(name);
                current.addGroup(next);
                daoFactory.getTemplateGroupDao().save(next);
            }
            current = next;
        }
    }
 
    /**
     * Gets the root template group for the given workspace ID, creating it if necessary.
     * 
     * @param username the name of the user.
     * @param name the template group name.
     * @return the template group.
     */
    private TemplateGroup getRootTemplateGroup(String username, String name) {
        Workspace workspace = getWorkspaceForUser(username);
        TemplateGroup templateGroup = getRootTemplateGroupForWorkspace(workspace);
        if (templateGroup == null) {
            templateGroup = UnitTestUtils.createTemplateGroup(name);
            daoFactory.getTemplateGroupDao().save(templateGroup);
            workspace.setRootAnalysisGroupId(templateGroup.getHid());
        }
        else if (!templateGroup.getName().equals(name)) {
            throw new WorkflowException("mismatched names for root template group for user " + username);
        }
        return templateGroup;
    }

    /**
     * Finds the workspace ID for the given username.
     * 
     * @param username the name of the user.
     * @return the workspace ID.
     */
    private long getWorkspaceIdFor(String username) {
        return getWorkspaceForUser(username).getId();
    }

    /**
     * Finds the workspace for the given username.
     * 
     * @param username the name of the user.
     * @return the workspace.
     */
    private Workspace getWorkspaceForUser(final String username) {
        return ListUtils.first(new Predicate<Workspace>() {
            @Override
            public Boolean call(Workspace arg) {
                return arg.getUser().getUsername().equals(username);
            }
        }, daoFactory.getMockWorkspaceDao().getSavedObjects());
    }

    /**
     * Resolves a template group name for testing.
     * 
     * @param username the name of the user.
     * @param path the path to the template group, not including the root template group.
     * @return the resolved template group or null if the path couldn't be resolved.
     */
    private TemplateGroup testResolve(String username, String... path) {
        Workspace workspace = getWorkspaceForUser(username);
        TemplateGroup curr = daoFactory.getTemplateGroupDao().findByHid(workspace.getRootAnalysisGroupId());
        for (String name : path) {
            if (curr == null) {
                return null;
            }
            curr = curr.getSubgroup(name);
        }
        return curr;
    }
}
