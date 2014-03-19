package org.iplantc.persistence.dto.workspace;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.iplantc.persistence.dto.user.User;

/**
 * Stores information about private and public workspaces.
 */
@NamedQueries({
    @NamedQuery(name = "Workspace.findById", query = "from Workspace where id = :id"),
    @NamedQuery(name = "Workspace.findByUser", query = "from Workspace where user = :user"),
    @NamedQuery(name = "Workspace.findPublicWorkspaces", query = "from Workspace where isPublic is true")})
@Entity
@Table(name = "workspace")
public class Workspace implements Serializable {
    /**
     * The internal workspace identifier.
     */
    private long id;

    /**
     * The root analysis group identifier for the workspace.
     */
    private Long rootAnalysisGroupId;

    /**
     * True if the workspace is visible to everyone.
     */
    private boolean isPublic;

	/**
	 * User that owns this workspace.
	 */
	private User user;

    /**
     * True if the workspace was just created.
     */
    private boolean isNew = false;

    /**
     * @return the workspace identifier.
     */
    @SequenceGenerator(name = "workspace_id_seq", sequenceName = "workspace_id_seq")
    @GeneratedValue(generator = "workspace_id_seq")
    @Id
    public long getId() {
        return id;
    }

    /**
     * @param id the new workspace identifier.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the identifier of the root analysis group for this workspace.
     */
    @Column(name = "root_analysis_group_id", nullable = true)
    public Long getRootAnalysisGroupId() {
        return rootAnalysisGroupId;
    }

    /**
     * @param rootAnalysisGroupId the root analysis group identifier.
     */
    public void setRootAnalysisGroupId(Long rootAnalysisGroupId) {
        this.rootAnalysisGroupId = rootAnalysisGroupId;
    }

    /**
     * @return true if this workspace is public.
     */
    @Column(name = "is_public", nullable = true)
    public boolean getIsPublic() {
        return isPublic;
    }

    /**
     * @param isPublic true if this workspace is public.
     */
    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

	@OneToOne
	@JoinColumn(name = "user_id")
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

    @Transient
    public boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }
}
