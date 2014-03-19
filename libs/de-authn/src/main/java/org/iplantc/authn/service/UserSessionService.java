package org.iplantc.authn.service;

import org.iplantc.authn.user.User;

public interface UserSessionService {

	/**
	 * Get the User associated with the current thread, i.e. session.
	 * @return the user
	 */
	User getUser();

}