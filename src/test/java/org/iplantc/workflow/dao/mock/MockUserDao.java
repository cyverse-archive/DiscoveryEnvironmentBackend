package org.iplantc.workflow.dao.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.iplantc.persistence.dao.user.UserDao;
import org.iplantc.persistence.dto.user.User;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class MockUserDao implements UserDao {
    private HashMap<Long, User> users;
    private HashMap<String, User> usernameIndex;
    private long currentId;
    
    public MockUserDao() {
        users = new HashMap<Long, User>();
        usernameIndex = new HashMap<String, User>();
        currentId = 1;
    }
    
    @Override
    public User findByUsername(String username) {
        return usernameIndex.get(username);
    }

    @Override
    public void save(User object) {
        object.setId(currentId);
        currentId++;
        
        users.put(object.getId(), object);
        usernameIndex.put(object.getUsername(), object);
    }

    @Override
    public void delete(User object) {
        users.remove(object.getId());
        usernameIndex.remove(object.getUsername());
    }

    @Override
    public void deleteAll(Collection<User> objects) {
        for (User user : objects) {
            delete(user);
        }
    }

    @Override
    public void deleteById(long id) {
        delete(findById(id));
    }

    @Override
    public User findById(long id) {
        return users.get(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<User>(users.values());
   }
}
