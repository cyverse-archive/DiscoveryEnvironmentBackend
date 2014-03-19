package org.iplantc.workflow.service.dto;

import java.util.Collections;
import java.util.List;
import net.sf.json.JSONObject;

/**
 * Represents a full path to an analysis category.
 * 
 * @author Dennis Roberts
 */
public class CategoryPath extends AbstractDto {

    /**
     * The name of the user that owns the workspace.
     */
    @JsonField(name = "username")
    protected String username;

    /**
     * The path to the category, relative to the user's workspace.
     */
    @JsonField(name = "path")
    protected List<String> path;

    /**
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the list of category names.
     */
    public List<String> getPath() {
        return Collections.unmodifiableList(path);
    }

    /**
     * @param username the name of the user that owns the workspace.
     * @param path the path to the category, relative to the user's workspace.
     */
    public CategoryPath(String username, List<String> path) {
        this.username = username;
        this.path = path;
    }

    /**
     * @param json a JSON object representing the category path.
     */
    public CategoryPath(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str a JSON string representing the category path.
     */
    public CategoryPath(String str) {
        fromString(str);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CategoryPath other = (CategoryPath) obj;
        if ((this.username == null) ? (other.username != null) : !this.username.equals(other.username)) {
            return false;
        }
        if (this.path != other.path && (this.path == null || !this.path.equals(other.path))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.username != null ? this.username.hashCode() : 0);
        hash = 29 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }
}
