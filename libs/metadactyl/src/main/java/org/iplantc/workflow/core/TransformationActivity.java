package org.iplantc.workflow.core;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.iplantc.persistence.NamedAndUnique;
import org.iplantc.persistence.dto.data.IntegrationDatum;
import org.iplantc.persistence.dto.listing.JobType;
import org.iplantc.persistence.dto.listing.PipelineCandidate;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.workflow.util.Lambda;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * This class groups a set of Transformations that are going to be
 * executed as a unit of work.
 * 
 * @author Juan Antonio Raygoza Garay
 */
public class TransformationActivity implements NamedAndUnique, PipelineCandidate {

    private List<TransformationStep> steps = new LinkedList<TransformationStep>();

    private List<InputOutputMap> mappings = new LinkedList<InputOutputMap>();
    private Set<Rating> ratings;
    private Set<TransformationActivityReference> references;
    private Set<TemplateGroup> suggestedGroups;

    /*
     * Inputs and outputs will be inherited by the first and last
     * transformations in the workflow.
     */
    private List<DataObject> input = new LinkedList<DataObject>();

    private List<DataObject> output = new LinkedList<DataObject>();

    private long hid;

    public long workspaceId;

    private String type = "";

    private boolean deleted;

    private String id;

    private String name;

    private String description;

    private IntegrationDatum integrationDatum;
    
    private String wikiurl;
    
    private Date integrationDate;

    private Date editedDate;

    private Set<String> jobTypeNames;

    private boolean disabled;

    public TransformationActivity() {
    }

    public List<TransformationStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TransformationStep> steps) {
        this.steps = steps;
    }

    public void addStep(TransformationStep step) {
        steps.add(step);
    }

    public TransformationStep step(int index) {
        return steps.get(index);
    }

    public List<DataObject> getInput() {
        return input;
    }

    public void setInput(List<DataObject> input) {
        this.input = input;
    }

    public List<DataObject> getOutput() {
        return output;
    }

    public void setOutput(List<DataObject> output) {
        this.output = output;
    }

    public List<InputOutputMap> getMappings() {
        return mappings;
    }

    public void setMappings(List<InputOutputMap> mappings) {
        this.mappings = mappings;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Set<String> getJobTypeNames() {
        return jobTypeNames;
    }

    public void setJobTypeNames(Set<String> jobTypeNames) {
        this.jobTypeNames = jobTypeNames;
    }

    public void setJobTypeNames(List<String> jobTypeNames) {
        this.jobTypeNames = new HashSet<String>(jobTypeNames);
    }

    @Override
    public long getStepCount() {
        return steps.size();
    }

    public TransformationStep getStepByName(String name) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getName().equals(name)) {
                return steps.get(i);
            }
        }
        return null;
    }

    public void addMapping(InputOutputMap map) {

        mappings.add(map);
    }

    public boolean isSourceInMapping(final String step, final String property) {
        return ListUtils.any(new Predicate<InputOutputMap>() {
            @Override
            public Boolean call(InputOutputMap arg) {
                return arg.getSource().getName().equals(step) && arg.containsPropertyAsSource(property);
            }
        }, mappings);
    }

    public boolean isTargetInMapping(final String step, final String property) {
        return ListUtils.any(new Predicate<InputOutputMap>() {
            @Override
            public Boolean call(InputOutputMap arg) {
                return arg.getTarget().getName().equals(step) && arg.containsPropertyAsTarget(property);
            }
        }, mappings);
    }

    public ArrayList<InputOutputMap> getMappingsForTargetStep(String step) {
        ArrayList<InputOutputMap> tmappings = new ArrayList<InputOutputMap>();

        for (InputOutputMap inOutMap : mappings) {
            if (inOutMap.getTarget().getName().equals(step)) {
                tmappings.add(inOutMap);
            }
        }
        return tmappings;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransformationActivity other = (TransformationActivity) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        validateFieldLength(this.getClass(), "type", type, 255);
        this.type = type;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void copy(TransformationActivity other) {
        copySteps(other.getSteps());
        copyMappings(other.getMappings());
        copyInput(other.getInput());
        copyOutput(other.getOutput());
        workspaceId = other.getWorkspaceId();
        type = other.getType();
        disabled = other.isDisabled();
        deleted = other.isDeleted();

        name = other.getName();
        description = other.getDescription();
    }

    private void copyOutput(List<DataObject> source) {
        output.clear();
        output.addAll(source);
    }

    private void copyInput(List<DataObject> source) {
        input.clear();
        input.addAll(source);
    }

    private void copyMappings(List<InputOutputMap> source) {
        mappings.clear();
        mappings.addAll(source);
    }

    private void copySteps(List<TransformationStep> source) {
        steps.clear();
        steps.addAll(source);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        validateFieldLength(this.getClass(), "description", description, 255);
        this.description = description;
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

    public long getHid() {
        return hid;
    }

    public void setHid(long hid) {
        this.hid = hid;
    }

    public IntegrationDatum getIntegrationDatum() {
        return integrationDatum;
    }

    public void setIntegrationDatum(IntegrationDatum integrationDatum) {
        this.integrationDatum = integrationDatum;
    }

    public Set<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(Set<Rating> ratings) {
        this.ratings = ratings;
    }

    public Set<TransformationActivityReference> getReferences() {
        return references;
    }

    public void setReferences(Set<TransformationActivityReference> references) {
        this.references = references;
    }

    public String getWikiurl() {
        return wikiurl;
    }

    public void setWikiurl(String wikiurl) {
        validateFieldLength(this.getClass(), "wikiurl", wikiurl, 1024);
        this.wikiurl = wikiurl;
    }

    public Set<TemplateGroup> getSuggestedGroups() {
        return suggestedGroups;
    }

    public void setSuggestedGroups(Set<TemplateGroup> suggestedGroups) {
        this.suggestedGroups = suggestedGroups;
    }

    public Date getIntegrationDate() {
        return integrationDate;
    }

    public void setIntegrationDate(Date integrationDate) {
        this.integrationDate = integrationDate;
    }

    public Date getEditedDate() {
        return editedDate;
    }

    public void setEditedDate(Date editedDate) {
        this.editedDate = editedDate;
    }

    /**
     * Gets the Average rating for this TransformationActivity.
     * 
     * @return
     *  Average user rating.
     */
    public double getAverageRating() {
        double average = 0.0;
        
        // Average Rating
        if(!getRatings().isEmpty()) {
            double ratingSum = 0.0;
            for (Rating rating : getRatings()) {
                ratingSum += rating.getRaiting();
            }
            
            average = ratingSum / getRatings().size();
        }
        
        return average;
    }

    /**
     * Gets the list of template identifiers associated with the analysis.
     * 
     * @return the list of template identifiers.
     */
    public List<String> getTemplateIds() {
        return ListUtils.mapDiscardingNulls(new Lambda<TransformationStep, String>() {
            @Override
            public String call(TransformationStep step) {
                return step.getTemplateId();
            }
        }, steps);
    }

    public Set<JobType> getJobTypes() {
        Set<JobType> result = EnumSet.noneOf(JobType.class);
        for (String jobTypeName : jobTypeNames) {
            result.add(JobType.fromString(jobTypeName));
        }
        return result;
    }

    @Override
    public JobType getOverallJobType() {
        JobType jobType = null;
        for (JobType currType : getJobTypes()) {
            if (jobType == null) {
                jobType = currType;
            }
            else if (currType != jobType) {
                jobType = JobType.MIXED;
                break;
            }
        }
        return jobType;
    }

    @Override
    public String toString() {
        return "TransformationActivity{" + "id=" + id + '}';
    }

    public boolean isMultistep() {
        return steps.size() > 1;
    }
}
