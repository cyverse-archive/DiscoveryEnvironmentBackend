package org.iplantc.persistence.dto.user;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents a User within the data store.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
@Entity
@Table(name = "users")
@NamedQueries({
	@NamedQuery(name = "User.findById", query = "from User where id = :id"),
	@NamedQuery(name = "User.findByUsername", query = "from User where username = :username")
})
public class User implements Serializable {
	private Long id;
	private String username;
	
	public User() {
		
	}

	@Column(name = "username")
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@SequenceGenerator(name="users_id_seq", sequenceName="users_id_seq")
	@GeneratedValue(generator="users_id_seq")
	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
