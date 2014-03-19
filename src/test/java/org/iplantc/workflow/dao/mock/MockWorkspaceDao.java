package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.iplantc.persistence.dao.WorkspaceDao;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.util.EqualsPredicate;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * A mock workspace data access object for testing.
 *
 * @author Dennis Roberts
 */
public class MockWorkspaceDao implements WorkspaceDao {

    /**
     * The next workspace identifier to assign.
     */
    private long nextId = 1000;

    /**
     * The list of objects that have been saved.
     */
    List<Workspace> savedObjects = new ArrayList<Workspace>();

    /**
     * @return the list of objects that have been saved.
     */
    public List<Workspace> getSavedObjects() {
        return Collections.unmodifiableList(savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Workspace> findPublicWorkspaces() {
        return ListUtils.filter(new Predicate<Workspace>() {
            @Override
            public Boolean call(Workspace arg) {
                return arg.getIsPublic();
            }
        }, savedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(Workspace workspace) {
        int index = ListUtils.firstIndex(new EqualsPredicate(workspace), savedObjects);
        if (index < 0) {
            workspace.setId(nextId++);
            savedObjects.add(workspace);
        }
        else {
            savedObjects.set(index, workspace);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Workspace workspace) {
        savedObjects.remove(workspace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Collection<Workspace> workspaces) {
        savedObjects.removeAll(workspaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(long id) {
        Workspace workspace = findById(id);
        if (workspace != null) {
            savedObjects.remove(workspace);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workspace findById(final long id) {
        return ListUtils.first(new Predicate<Workspace>() {
            @Override
            public Boolean call(Workspace arg) {
                return id == arg.getId();
            }
        }, savedObjects);
    }

    @Override
    public void deleteByUser(User user) {
        Workspace workspace = findByUser(user);
        if (workspace != null) {
            delete(workspace);
        }
    }

    @Override
    public Workspace findByUser(final User user) {
        return ListUtils.first(new Predicate<Workspace>() {
            @Override
            public Boolean call(Workspace arg) {
                return user.equals(arg.getUser());
            }
        }, savedObjects);
    }

    @Override
    public List<Workspace> findAll() {
        return savedObjects;
    }
}
