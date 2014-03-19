package org.iplantc.workflow.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;

/**
 * A list of failed analysis categorizations.
 * 
 * @author Dennis Roberts
 */
public class FailedCategorizationList extends AbstractDto implements Iterable<FailedCategorization> {

    /**
     * The list of categories.
     */
    @JsonField(name = "failed_categorizations")
    protected List<FailedCategorization> failedCategorizations;

    /**
     * @return the list of failed categorizations.
     */
    public List<FailedCategorization> getFailedCategorizations() {
        return Collections.unmodifiableList(failedCategorizations);
    }

    /**
     * @param failedCategorization the failed categorization to add.
     */
    public void addCategory(FailedCategorization failedCategorization) {
        failedCategorizations.add(failedCategorization);
    }

    /**
     * The default constructor.
     */
    public FailedCategorizationList() {
        failedCategorizations = new ArrayList<FailedCategorization>();
    }

    /**
     * @param json the JSON object representing the failed categorization list.
     */
    public FailedCategorizationList(JSONObject json) {
        fromJson(json);
    }

    /**
     * @param str the JSON string representing the failed categorization list.
     */
    public FailedCategorizationList(String str) {
        fromString(str);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<FailedCategorization> iterator() {
        return failedCategorizations.iterator();
    }
}
