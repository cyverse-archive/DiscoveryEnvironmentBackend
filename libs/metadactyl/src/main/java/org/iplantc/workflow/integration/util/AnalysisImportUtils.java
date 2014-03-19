package org.iplantc.workflow.integration.util;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.TransformationActivityDao;

/**
 * Utility methods for importing analyses.
 *
 * @author Dennis Roberts
 */
public class AnalysisImportUtils {

    /**
     * Prevent instantiation.
     */
    private AnalysisImportUtils() {
    }

    /**
     * Searches for an existing analysis to update in the database and returns null if no existing analysis is found. An
     * analysis can be updated either by identifier or by name as long as the analysis to update can be uniquely
     * identified in the database. If update by name is selected and an analysis can't be uniquely identified then an
     * exception will be thrown.
     *
     * @param dao the data access object used to find the existing analysis.
     * @param id the analysis identifier.
     * @param name the analysis name.
     * @return the analysis or null if a matching analysis isn't found.
     * @throws WorkflowException if an analysis ID isn't specified and multiple analyses with the same name are found.
     */
    public static TransformationActivity findExistingAnalysis(TransformationActivityDao dao, String id, String name) {
        TransformationActivity analysis = null;
        if (!StringUtils.isBlank(id)) {
            analysis = dao.findById(id);
        }
        else {
            List<TransformationActivity> analyses = dao.findByName(name);
            if (analyses.size() > 1) {
                String msg = "Multiple apps named, " + name + ", were found.  Please specify an identifier to "
                        + "indicate which app should be updated or to insert a new app with the same name.  If "
                        + "you would like to insert a new app with the same name and have the identifier generated "
                        + "automatically, please specify \"auto-gen\" for the identifier.";
                throw new WorkflowException(msg);
            }
            analysis = analyses.isEmpty() ? null : analyses.get(0);
        }
        return analysis;
    }

    /**
     * Gets a date for a single timestamp string.
     *
     * @param timestamp the timestamp string.
     * @return the date.
     */
    public static Date getDate(String timestamp) {
        return getDate(timestamp, null);
    }

    /**
     * Gets a date for a single timestamp string, returning a default date if the timestamp string is null or blank.
     * 
     * @param timestamp the timestamp string.
     * @param defaultDate the default date to return.
     * @return the date.
     */
    public static Date getDate(String timestamp, Date defaultDate) {
        if (!StringUtils.isBlank(timestamp)) {
            try {
                return new Date(Long.parseLong(timestamp));
            }
            catch (Exception ignore) {
            }
        }
        return defaultDate;
    }
}
