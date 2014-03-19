package org.iplantc.workflow.service.dto;

import java.util.List;
import net.sf.json.JSONObject;
import org.iplantc.workflow.core.TransformationActivity;

/**
 * A data transfer object for categorized analyses.
 * 
 * @author Dennis Roberts
 */
public class CategorizedAnalysis extends AbstractDto {

    /**
     * The full path to the category.
     */
    @JsonField(name = "category_path")
    protected CategoryPath categoryPath;

    /**
     * The analysis in the category.
     */
    @JsonField(name = "analysis")
    protected AnalysisDto analysis;

    /**
     * @return the path to the category.
     */
    public CategoryPath getCategoryPath() {
        return categoryPath;
    }

    /**
     * @return the analysis DTO.
     */
    public AnalysisDto getAnalysis() {
        return analysis;
    }

    /**
     * @param username the name of the user.
     * @param categories the list of categories.
     * @param analysis the analysis.
     */
    public CategorizedAnalysis(String username, List<String> categories, TransformationActivity analysis) {
        this.categoryPath = new CategoryPath(username, categories);
        this.analysis = new AnalysisDto(analysis);
    }

    /**
     * @param json the JSON object representing the analysis category.
     */
    public CategorizedAnalysis(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str the JSON string representing the analysis category.
     */
    public CategorizedAnalysis(String str) {
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
        final CategorizedAnalysis other = (CategorizedAnalysis) obj;
        if (this.categoryPath != other.categoryPath &&
                (this.categoryPath == null || !this.categoryPath.equals(other.categoryPath))) {
            return false;
        }
        if (this.analysis != other.analysis && (this.analysis == null || !this.analysis.equals(other.analysis))) {
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
        hash = 59 * hash + (this.categoryPath != null ? this.categoryPath.hashCode() : 0);
        hash = 59 * hash + (this.analysis != null ? this.analysis.hashCode() : 0);
        return hash;
    }
}
