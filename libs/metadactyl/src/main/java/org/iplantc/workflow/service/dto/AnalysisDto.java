package org.iplantc.workflow.service.dto;

import net.sf.json.JSONObject;
import org.iplantc.workflow.core.TransformationActivity;

/**
 * A data transfer object for analyses.
 * 
 * @author Dennis Roberts
 */
public class AnalysisDto extends AbstractDto {

    /**
     * The analysis identifier.
     */
    @JsonField(name = "id")
    protected String id;

    /**
     * The analysis name.
     */
    @JsonField(name = "name")
    protected String name;

    /**
     * @return the analysis identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the analysis name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param analysis the analysis represented by the DTO.
     */
    public AnalysisDto(TransformationActivity analysis) {
        this.id = analysis.getId();
        this.name = analysis.getName();
    }

    /**
     * @param json the JSON object representing the DTO.
     */
    public AnalysisDto(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str the JSON string representing the DTO.
     */
    public AnalysisDto(String str) {
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
        final AnalysisDto other = (AnalysisDto) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 13 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
