package org.iplantc.persistence.dao.user;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.user.User;

/**
 *
 * @author Kris Healy &lt;healyk@iplantcollaborative.org&gt;
 */
public interface UserDao extends GenericDao<User> {
	/**
	 * Finds a user by their username.
     *
     * @param username the name of the user to search for.
     * @return the user.
	 */
	public User findByUsername(String username);
}
