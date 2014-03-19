package org.iplantc.workflow.user;

import org.apache.log4j.Logger;
import org.iplantc.authn.user.User;

/**
 * Details about an iPlant user (typically the current user).
 *
 * @author Dennis Roberts
 */
public class UserDetails {

    private static final Logger LOG = Logger.getLogger(UserDetails.class);

    /**
     * The fully qualified username.
     */
    private final String username;

    /**
     * The user's password if it's available.
     */
    private final String password;

    /**
     * The user's e-mail address.
     */
    private final String email;

    /**
     * The username without the qualifying information.
     */
    private final String shortUsername;

    /**
     * The user's first name.
     */
    private final String firstName;

    /**
     * The user's last name.
     */
    private final String lastName;

    /**
     * @return the user's e-mail address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the user's password if it's available.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the username without the qualifying information.
     */
    public String getShortUsername() {
        return shortUsername;
    }

    /**
     * @return the fully qualified username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the user's first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return the user's last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param user the user information from the user session service.
     */
    public UserDetails(User user) {
        username = user.getUsername();
        password = user.getPassword();
        email = user.getEmail();
        shortUsername = user.getShortUsername();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        LOG.debug("New User Details Created:\n"
                + "\tusername = " + username + "\n"
                + "\tpassword = " + password + "\n"
                + "\temail = " + email + "\n"
                + "\tshortUsername = " + shortUsername
                + "\tfirstName = " + firstName
                + "\tlastName = " + lastName);
    }

    /**
     * This constructor is used exclusively for unit testing.
     *
     * @param username the fully qualified username.
     * @param password the password if it's available.
     * @param email the user's e-mail address.
     * @param shortUsername the username without the qualifying information.
     * @param firstName the user's first name.
     * @param lastName the user's last name
     */
    public UserDetails(String username, String password, String email, String shortUsername, String firstName,
            String lastName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.shortUsername = shortUsername;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
