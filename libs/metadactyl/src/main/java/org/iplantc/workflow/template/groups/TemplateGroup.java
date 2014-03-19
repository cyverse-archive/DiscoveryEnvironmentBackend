package org.iplantc.workflow.template.groups;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import java.util.Set;
import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TemplateGroup implements NamedAndUnique {
    private long hid;

    private String id;

    private String name;

    private String description;

    private long workspaceId;

    private List<TemplateGroup> sub_groups = new LinkedList<TemplateGroup>();
    private Set<TransformationActivity> templates = new HashSet<TransformationActivity>();

    public long getHid() {
        return hid;
    }

    public void setHid(long hid) {
        this.hid = hid;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        validateFieldLength(this.getClass(), "id", id, 255);
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateFieldLength(this.getClass(), "name", name, 255);
        this.name = name;
    }

    public List<TemplateGroup> getSub_groups() {
        return sub_groups;
    }

    public void setSub_groups(List<TemplateGroup> sub_groups) {
        this.sub_groups = sub_groups;
    }

    public Set<TransformationActivity> getTemplates() {
        return templates;
    }

    public void setTemplates(Set<TransformationActivity> templates) {
        this.templates = templates;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        validateFieldLength(this.getClass(), "description", description, 255);
        this.description = description;
    }

    public void addGroup(TemplateGroup group) {
        sub_groups.add(group);
    }

    public void addTemplate(TransformationActivity template) {
        templates.add(template);
    }

    public void removeTemplate(TransformationActivity template) {
        templates.remove(template);
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public boolean containsGroup(String name) {
        for (TemplateGroup currentGroup : sub_groups) {
            if (currentGroup.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAnalysis(TransformationActivity analysis) {
        return templates.contains(analysis);
    }

    public TemplateGroup getSubgroup(String name) {
        for (TemplateGroup currentGroup : sub_groups) {
            if (currentGroup.getName().equals(name)) {
                return currentGroup;
            }
        }
        return null;
    }

    public boolean directlyContainsAnalysisWithId(String analysisId) {
        boolean retval = false;
        for (TransformationActivity analysis : templates) {
            if (analysis.getId().equals(analysisId)) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public boolean containsActiveAnalyses() {
        boolean retval = false;
        if (directlyContainsActiveAnalyses()) {
            retval = true;
        }
        else if (indirectlyContainsActiveAnalyses()) {
            retval = true;
        }
        return retval;
    }

    private boolean indirectlyContainsActiveAnalyses() {
        boolean retval = false;
        for (TemplateGroup subgroup : sub_groups) {
            if (subgroup.containsActiveAnalyses()) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    private boolean directlyContainsActiveAnalyses() {
        boolean retval = false;
        for (TransformationActivity analysis : templates) {
            if (!analysis.isDeleted()) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("hid", hid);
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("analyses", analysesToJson());
        return json;
    }

    private JSONArray analysesToJson() throws JSONException {
        JSONArray array = new JSONArray();
        for (TransformationActivity analysis : templates) {
            array.put(analysisToJson(analysis));
        }
        return array;
    }

    private JSONObject analysisToJson(TransformationActivity analysis) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("hid", analysis.getHid());
        json.put("id", analysis.getId());
        json.put("name", analysis.getName());
        return json;
    }

    @Override
    public String toString() {
        try {
            return toJson().toString(4);
        }
        catch (JSONException e) {
            return "";
        }
    }

    public int countActiveTemplates() {
        int count = countLocalActiveTemplates();
        for (TemplateGroup group : sub_groups) {
            count += group.countActiveTemplates();
        }
        return count;
    }

    private int countLocalActiveTemplates() {
        return ListUtils.count(new Predicate<TransformationActivity>() {
            @Override
            public Boolean call(TransformationActivity template) {
                return !template.isDeleted();
            }
        }, templates);
    }

    public boolean inPublicWorkspace(DaoFactory daoFactory) {
        Workspace workspace = daoFactory.getWorkspaceDao().findById(workspaceId);
        return workspace == null ? false : workspace.getIsPublic();
    }
}
