package org.iplantc.workflow.core;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.iplantc.persistence.dto.user.User;

/**
 * Represents a Vote within the system.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
@Entity
@Table(name = "ratings")
@NamedQueries({
    @NamedQuery(name = "Rating.findById", query = "FROM Rating WHERE id = :id"),
    @NamedQuery(name = "Rating.findByUser", query = "FROM Rating WHERE :user = user")
})
public class Rating implements Serializable {
	private Long id;
	private User user;
	private TransformationActivity transformationActivity;
	private Integer raiting;
    private Long commentId;
	
	public Rating() {
		
	}
	
	@SequenceGenerator(name="ratings_id_seq", sequenceName="ratings_id_seq")
	@GeneratedValue(generator="ratings_id_seq")
	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "transformation_activity_id", updatable = false, insertable = false)
	public TransformationActivity getTransformationActivity() {
		return transformationActivity;
	}

	public void setTransformationActivity(TransformationActivity transformationActivity) {
		this.transformationActivity = transformationActivity;
	}

	@Column(name = "rating", nullable = false)
	public Integer getRaiting() {
		return raiting;
	}

	public void setRaiting(Integer raiting) {
		this.raiting = raiting;
	}

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    @Column(name = "comment_id")
    public Long getCommentId() {
        return commentId;
    }

    @ManyToOne
	@JoinColumn(name = "user_id")
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
