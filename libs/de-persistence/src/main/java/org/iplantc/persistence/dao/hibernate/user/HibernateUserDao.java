package org.iplantc.persistence.dao.hibernate.user;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dao.user.UserDao;
import org.iplantc.persistence.dto.user.User;

/**
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class HibernateUserDao extends AbstractHibernateDao<User> implements UserDao {
	public HibernateUserDao(Session session) {
		super(User.class, session);
	}
	
	@Override
	public User findByUsername(String username) {
		Query query = getNamedQuery("findByUsername");
		query.setParameter("username", username);
		
		return (User)query.uniqueResult();
	}
}
