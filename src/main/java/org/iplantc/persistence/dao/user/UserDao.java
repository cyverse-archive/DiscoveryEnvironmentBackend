package org.iplantc.persistence.dao.user;

import org.iplantc.persistence.dao.GenericDao;
import org.iplantc.persistence.dto.user.User;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public interface UserDao extends GenericDao<User> {
	/**
	 * Finds a user by their username.
	 */
	public User findByUsername(String username);
}
