package org.iplantc.workflow.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;

/**
 * A list of analysis categories.
 * 
 * @author Dennis Roberts
 */
public class AnalysisCategoryList extends AbstractDto implements Iterable<CategorizedAnalysis> {

    /**
     * The list of categories.
     */
    @JsonField(name = "categories")
    protected List<CategorizedAnalysis> categories;

    /**
     * @return the list of categories.
     */
    public List<CategorizedAnalysis> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    /**
     * @param category the category to add.
     */
    public void addCategory(CategorizedAnalysis category) {
        categories.add(category);
    }

    /**
     * The default constructor.
     */
    public AnalysisCategoryList() {
        categories = new ArrayList<CategorizedAnalysis>();
    }

    /**
     * @param json the JSON object representing the analysis list.
     */
    public AnalysisCategoryList(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str the JSON string representing the analysis list.
     */
    public AnalysisCategoryList(String str) {
        fromString(str);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<CategorizedAnalysis> iterator() {
        return categories.iterator();
    }
}
