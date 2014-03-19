package org.iplantc.authn.exception;

/**
 * This occurs when trying to obtain the user for the current session and
 * we are unable to.  This should never happen because there should
 * always be a user associated with every service invocation.
 * @author Donald A. Barre
 */
@SuppressWarnings("serial")
public class UserNotFoundException extends RuntimeException {

	private static final String ERROR_MSG = "Unable to find the user for the current session.";

	public UserNotFoundException() {
		super(ERROR_MSG);
	}
}
